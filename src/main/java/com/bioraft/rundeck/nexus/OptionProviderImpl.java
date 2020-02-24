/*
 * Copyright 2019 BioRAFT, Inc. (https://bioraft.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bioraft.rundeck.nexus;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.dtolabs.rundeck.plugins.option.OptionValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import okhttp3.Request.Builder;

/**
 * Expands three-part semantic versions to handle cases where there are more
 * than 3 parts and where build specifier may be attached by "_" or "+".
 *
 * It would really just be better if people only used semantic versions.
 *
 * @author Karl DeBisschop <kdebisschop@gmail.com>
 * @since 2019-12-26
 */
public class OptionProviderImpl {

	static final String CONTINUATION_TOKEN = "continuationToken";

	private OkHttpClient client;

	private Map<String, String> config;

	public OptionProviderImpl() {
		this.client = new OkHttpClient();
	}

	public OptionProviderImpl(OkHttpClient client) {
		this.client = client;
	}

	public List<OptionValue> getOptionValues(Map<String, String> config) {
		this.config = config;

		List<OptionValue> optionValues = new ArrayList<>();
		if (!config.containsKey("endpointHost")) {
			String message = "Configure project.plugin.OptionValues.Nexus3OptionProvider.endpointHost";
			optionValues.add(new ErrorOptionValue(message));
			return optionValues;
		}

		List<String> imageList = nexusSearch();

		Map<String, BranchOrVersion> seenBranches = new TreeMap<>();
		Map<String, BranchOrVersion> seenReleases = new TreeMap<>();

		BranchOrVersion latest = null;

		for (String path : imageList) {
			BranchOrVersion current = new BranchOrVersion(path);
			String versionOrBuild = current.getVersion();
			if (current.isVersion()) {
				updateBranchOrVersionMap(current, versionOrBuild, seenReleases);
				// Store the most recent version to render as the first entry.
				// Older versions will be rendered at the end of the list.
				if (current.compareTo(latest) > 0) {
					latest = current;
				}
			} else {
				updateBranchOrVersionMap(current, versionOrBuild, seenBranches);
			}
		}

		if (latest != null) {
			optionValues.add(new DockerImageOptionValue(latest));
		}

		Iterator<Entry<String, BranchOrVersion>> seen;

		seen = entriesSortedByValues(seenBranches).iterator();
		while (seen.hasNext()) {
			optionValues.add(new DockerImageOptionValue(seen.next().getValue()));
		}

		seen = entriesSortedByValues(seenReleases).iterator();
		while (seen.hasNext()) {
			optionValues.add(new DockerImageOptionValue(seen.next().getValue()));
		}

		return optionValues;
	}

	/**
	 * Update the Branches Map.
	 * @param current The branch or version object we are considering.
	 * @param versionOrBuild The version or build of the object we are considering.
	 */
	private void updateBranchOrVersionMap(BranchOrVersion current, String versionOrBuild, Map<String, BranchOrVersion> map) {
		// If we are already tracking the branch, check to ensure this is newer before saving.
		// Otherwise, it is a new branc to us so start tracking it.
		if (map.containsKey(versionOrBuild)) {
			if (current.compareTo(map.get(versionOrBuild)) > 0) {
				map.put(versionOrBuild, current);
			}
		} else {
			map.put(versionOrBuild, current);
		}
	}

	/**
	 * Sorts versions or branches numerically by value instead of key using the compareTo function of the value object.
	 *
	 * @param map The map of BranchOrVersion objects, keyed by String.
	 * @return A list of assets sorted by VersionOrString.
	 */
	static SortedSet<Map.Entry<String, BranchOrVersion>> entriesSortedByValues(
			Map<String, BranchOrVersion> map) {
		SortedSet<Map.Entry<String, BranchOrVersion>> sortedEntries =
				new TreeSet<>((e1, e2) -> e1.getValue().compareTo(e2.getValue()));
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}

	/**
	 * Perform the Nexus API request.
	 *
	 * This is really just syntactic sugar for the initial request (with no
	 * continuation token). But it adds some clarity.
	 *
	 * @return An ArrayList of path strings.
	 */
	private ArrayList<String> nexusSearch() {
		return nexusSearch(null);
	}

	/**
	 * Perform the Nexus API request, including continuation for larger result sets.
	 *
	 * Since all we need to prepare the option list is path, extract that from the
	 * JsonNode and return just a list of strings in the interest of conserving
	 * system resources.
	 *
	 * @param continuationToken Continuation token from previous request, null on
	 *                          first request.
	 *
	 * @return An ArrayList of path strings.
	 */
	private ArrayList<String> nexusSearch(String continuationToken) {
		ArrayList<String> itemList = new ArrayList<>();
		String endpointScheme = config.get("endpointScheme");
		String endpointHost = config.get("endpointHost");
		String endpointPath = config.get("endpointPath");
		String endpoint = endpointScheme + "://" + endpointHost + endpointPath;
		HttpUrl.Builder urlBuilder = buildUrl(endpoint, continuationToken);
		Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
		if (config.containsKey("user") && config.containsKey("password")) {
			requestBuilder.addHeader("Authorization", Credentials.basic(config.get("user"), config.get("password")));
		}
		Request request = requestBuilder.build();
		Response response;
		String json;
		try {
			response = client.newCall(request).execute();
			ResponseBody body = response.body();
			assert body != null;
			json = body.string();
			if (json.length() == 0) {
				return new ArrayList<>();
			}
		} catch (NullPointerException | IOException e) {
			return new ArrayList<>();
		}
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode tree;
		try {
			tree = objectMapper.readTree(json);
		} catch (JsonProcessingException e1) {
			return itemList;
		}
		Iterator<JsonNode> items = tree.get("items").elements();
		while (items.hasNext()) {
			itemList.add(items.next().get("path").asText());
		}
		if (tree.has(CONTINUATION_TOKEN)) {
			String token = tree.path(CONTINUATION_TOKEN).asText();
			if (token.length() > 0) {
				ArrayList<String> nextItems = nexusSearch(token);
				if (nextItems != null) {
					itemList.addAll(nextItems);
				}
			}
		}

		return itemList;
	}

	private HttpUrl.Builder buildUrl(String endpoint, String continuationToken) {
		HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(endpoint)).newBuilder();
		urlBuilder.addQueryParameter("repository", config.get("repository"));
		urlBuilder.addQueryParameter("name", config.get("componentName"));
		// For docker, version is the docker tag.
		urlBuilder.addQueryParameter("sort", "version");
		if (config.containsKey("componentVersion")) {
			urlBuilder.addQueryParameter("version", config.get("componentVersion"));
		}
		if (continuationToken != null) {
			urlBuilder.addQueryParameter(CONTINUATION_TOKEN, continuationToken);
		}
		return urlBuilder;
	}

	/**
	 * Provides a means of informing users that the Nexus host still needs to be
	 * configured.
	 * 
	 * All methods are straightforward implementations of the interface.
	 */
	static class ErrorOptionValue implements OptionValue {
		String name;
		String value;

		public ErrorOptionValue(String message) {
			name = message;
			value = message;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
	}

	/**
	 * Provides the primary means of adding artifacts to the list of OptionValues.
	 */
	static class DockerImageOptionValue implements OptionValue {
		String name;
		String value;

		public DockerImageOptionValue(BranchOrVersion asset) {
			this.name = asset.toString();
			this.value = this.name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
	}
}
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.option.OptionValue;
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin;

import okhttp3.OkHttpClient;

@Plugin(name = Nexus3OptionProvider.PLUGIN_NAME, service = ServiceNameConstants.OptionValues)
@PluginDescription(title = "Nexus3 Images", description = "Filtered and sorted docker images on nexus server.")
public class Nexus3OptionProvider implements OptionValuesPlugin {

	public static final String PLUGIN_NAME = "Nexus3OptionProvider";

	private OkHttpClient client;

	@PluginProperty(title = "Endpoint scheme", description = "Nexus server scheme", required = true, defaultValue = "https", scope = PropertyScope.Project)
	private String endpointScheme;

	@PluginProperty(title = "Endpoint host", description = "Nexus server hostname", defaultValue = "", scope = PropertyScope.Project)
	private String endpointHost;

	@PluginProperty(title = "Endpoint path", description = "Nexus path with leading slash", required = true, defaultValue = "/service/rest/v1/search/assets", scope = PropertyScope.Project)
	private String endpointPath;

	@PluginProperty(title = "User", description = "Nexus server user name", defaultValue = "", scope = PropertyScope.Project)
	private String user;
	
	@PluginProperty(title = "Password", description = "Nexus server password", defaultValue = "", scope = PropertyScope.Project)
	private String password;
	
	@PluginProperty(title = "Repository", description = "Nexus repository", required = true, defaultValue = "docker", scope = PropertyScope.Project)
	private String repository;

	@PluginProperty(title = "Component name", description = "Nexus component name", required = true, defaultValue = "*", scope = PropertyScope.Project)
	private String componentName;

	@PluginProperty(title = "Component version", description = "Nexus component version", scope = PropertyScope.Project)
	private String componentVersion;

	public Nexus3OptionProvider() {
		this.client = new OkHttpClient();
	}

	public Nexus3OptionProvider(OkHttpClient client) {
		this.client = client;
	}

	Map<String, String> config;

	@Override
	public List<OptionValue> getOptionValues(Map configuration) {
		config = configuration;

		setVariable(configuration,"endpointScheme", endpointScheme);
		setVariable(configuration, "endpointHost", endpointHost);
		setVariable(configuration,"endpointPath", endpointPath);
		setVariable(configuration,"user", user);
		setVariable(configuration,"password", password);
		setVariable(configuration,"repository", repository);
		setVariable(configuration,"componentName", componentName);
		setVariable(configuration,"componentVersion",componentVersion);

		OptionProviderImpl worker = new OptionProviderImpl(client);
		return worker.getOptionValues(config);
	}

	private void setVariable(Map configuration, String variableName, String variable) {
		if (!configuration.containsKey(variableName) && variable != null && variable.length() > 0) {
			config.put(variableName, variable);
		}
	}
}
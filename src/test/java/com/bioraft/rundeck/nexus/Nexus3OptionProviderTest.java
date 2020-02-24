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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.plugins.option.OptionValue;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Tests for Nexus3OptionProvider.
 *
 * @author Karl DeBisschop <kdebisschop@gmail.com>
 * @since 2019-12-11
 */
@RunWith(MockitoJUnitRunner.class)
public class Nexus3OptionProviderTest {

	Nexus3OptionProvider plugin;

	@Mock
	OkHttpClient client;

	Request request;

	@Mock
	Call call;

	Map<String, String> configuration;

	@Before
	public void setUp() {
		configuration = Stream
				.of(new String[][] { { "endpointScheme", "https" }, { "endpointHost", "nexus.example.com" },
						{ "endpointPath", "/service/rest/v1/search/assets" }, { "repository", "docker" },
						{ "componentName", "*" }, { "componentVersion", "*" } })
				.collect(Collectors.toMap(data -> data[0], data -> data[1]));
	}

	@Test
	public void testConstructor() {
		Nexus3OptionProvider subject = new Nexus3OptionProvider();
		assertNotNull(subject);
	}

	@Test
	public void testConstructor2() {
		OptionProviderImpl subject = new OptionProviderImpl();
		assertNotNull(subject);
	}

	public void testConfiguration() throws IOException{
		when(client.newCall(any())).thenReturn(call);
		ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
		String json = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		provider.getOptionValues(configuration);

		verify(client).newCall(requestCaptor.capture());
		Request request = requestCaptor.getValue();
		assertEquals("https://nexus.example.com//service/rest/v1/search/assets", request.url().toString());
	}

	@Test
	public void testAuthHeader() {
		when(client.newCall(any())).thenReturn(call);
		OptionProviderImpl subject = new OptionProviderImpl();
		configuration.put("user", "user");
		configuration.put("password", "password");
		List<OptionValue> options = subject.getOptionValues(configuration);
		assertNotNull(options);
	}

	@Test
	public void clientCallThrowsException() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		when(call.execute()).thenThrow(new IOException());
		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);
		assertEquals(0, options.size());
	}

	@Test
	public void BadJsonFailedClientCall()  throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"},],},";
		when(call.execute()).thenReturn(response(json));
		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);
		assertEquals(0, options.size());
	}

	@Test
	public void NoJsonFailedClientCall()  throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "";
		when(call.execute()).thenReturn(response(json));
		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);
		assertEquals(0, options.size());
	}

	@Test
	public void showMessageIfNotConfigured() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		configuration.remove("endpointHost");
		String json = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(1, options.size());
		String message = "Configure project.plugin.OptionValues.Nexus3OptionProvider.endpointHost";
		assertEquals(message, options.get(0).getName());
		assertEquals(message, options.get(0).getValue());

		verify(call, times(0)).execute();
	}

	@Test
	public void callOnceIfNotContinued() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		provider.getOptionValues(configuration);

		verify(call, times(1)).execute();
	}

	@Test
	public void callTwiceIfContinued() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json1 = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}], \"continuationToken\" : \"string\"}";
		String json2 = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json1), response(json2));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		provider.getOptionValues(configuration);

		verify(call, times(2)).execute();
	}

	@Test
	public void callOnceIfContinuedTokenIsEmpty() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json1 = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}], \"continuationToken\" : \"\"}";
		String json2 = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json1), response(json2));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		provider.getOptionValues(configuration);

		verify(call, times(1)).execute();
	}

	@Test
	public void handleNullResultFrmContinuedToken() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json1 = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}], \"continuationToken\" : \"string\"}";
		String json2 = "{\"items\":[]}";
		when(call.execute()).thenReturn(response(json1), response(json2));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		verify(call, times(2)).execute();
		assertEquals(1, options.size());
	}

	@Test
	public void returnsOneItemForSingle() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[{\"path\": \"v2/COMP_NAME/manifests/sprint-11_4\"}]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(1, options.size());
		assertEquals("COMP_NAME:sprint-11_4", options.get(0).getName());
		assertEquals("COMP_NAME:sprint-11_4", options.get(0).getValue());
	}

	@Test
	public void returnsTwoItemsForDouble() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint-11_4") + "," + item("branch-foo_3") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(2, options.size());
		assertEquals("COMP_NAME:branch-foo_3", options.get(0).getName());
		assertEquals("COMP_NAME:sprint-11_4", options.get(1).getName());
	}

	@Test
	public void returnsTwoItemsForDoubleWithExtraBuilds() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint-11_3") + "," + item("sprint-11_4") + "," + item("branch-foo_3")
				+ "," + item("branch-foo_2") + "," + item("sprint-11_2") + "," + item("branch-foo_1") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(2, options.size());
		assertEquals("COMP_NAME:branch-foo_3", options.get(0).getName());
		assertEquals("COMP_NAME:sprint-11_4", options.get(1).getName());
	}

	@Test
	public void returnsFourItemsForOneBranchAndTwoReleases() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint-11_3") + "," + item("sprint-11_4") + "," + item("v3.1.2.3_4") + ","
				+ item("v3.1.2.2_1") + "," + item("v3.1.2.3_3") + "," + item("v3.1.2.2_2") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(4, options.size());
		assertEquals("COMP_NAME:v3.1.2.3_4", options.get(0).getName());
		assertEquals("COMP_NAME:sprint-11_4", options.get(1).getName());
		assertEquals("COMP_NAME:v3.1.2.2_2", options.get(2).getName());
		assertEquals("COMP_NAME:v3.1.2.3_4", options.get(3).getName());
	}

	@Test
	public void returnsFourItemsForOneBranchAndTwoSemanticReleases() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint-11_3") + "," + item("sprint-11_4") + "," + item("v1.2.3_4") + ","
				+ item("v1.2.2_1") + "," + item("v1.2.3_3") + "," + item("v1.2.2_2") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(4, options.size());
		assertEquals("COMP_NAME:v1.2.3_4", options.get(0).getName());
		assertEquals("COMP_NAME:sprint-11_4", options.get(1).getName());
		assertEquals("COMP_NAME:v1.2.2_2", options.get(2).getName());
		assertEquals("COMP_NAME:v1.2.3_4", options.get(3).getName());
	}

	@Test
	public void returnsFourItemsForOneBranchAndTwoBareSemanticReleases() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint_11-3") + "," + item("sprint_11-4") + "," + item("14.2.3-4") + ","
				+ item("2.2.2-1") + "," + item("14.2.3-3") + "," + item("2.2.2-2") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(4, options.size());
		assertEquals("COMP_NAME:14.2.3-4", options.get(0).getName());
		assertEquals("COMP_NAME:sprint_11-4", options.get(1).getName());
		assertEquals("COMP_NAME:2.2.2-2", options.get(2).getName());
		assertEquals("COMP_NAME:14.2.3-4", options.get(3).getName());
	}

	@Test
	public void sortsBuildNumbersNumerically() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint_11-13") + "," + item("sprint_11-4") + "," + item("1.2.3-4") + ","
				+ item("1.2.2-21") + "," + item("1.2.3-11") + "," + item("1.2.2-2") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(4, options.size());
		assertEquals("COMP_NAME:1.2.3-11", options.get(0).getName());
		assertEquals("COMP_NAME:sprint_11-13", options.get(1).getName());
		assertEquals("COMP_NAME:1.2.2-21", options.get(2).getName());
		assertEquals("COMP_NAME:1.2.3-11", options.get(3).getName());
	}

	@Test
	public void sortsIssuesNumerically() throws IOException {
		when(client.newCall(any())).thenReturn(call);
		String json = "{\"items\":[" + item("sprint_11-13") + "," + item("sprint_11-4") + "," + item("1.2.3-4") + ","
				+ item("1.2.2-21") + "," + item("1.2.3-11") + "," + item("ISSUE-234-one-issue-2") + ","
				+ item("ISSUE-234-one-issue-12") + "," + item("ISSUE-1000-another-issue-27") + ","
				+ item("ISSUE-1000-another-issue-13") + "," + item("1.2.2-2") + "]}";
		when(call.execute()).thenReturn(response(json));

		Nexus3OptionProvider provider = new Nexus3OptionProvider(client);
		List<OptionValue> options = provider.getOptionValues(configuration);

		assertEquals(6, options.size());
		assertEquals("COMP_NAME:1.2.3-11", options.get(0).getName());
		assertEquals("COMP_NAME:ISSUE-234-one-issue-12", options.get(1).getName());
		assertEquals("COMP_NAME:ISSUE-1000-another-issue-27", options.get(2).getName());
		assertEquals("COMP_NAME:sprint_11-13", options.get(3).getName());
		assertEquals("COMP_NAME:1.2.2-21", options.get(4).getName());
		assertEquals("COMP_NAME:1.2.3-11", options.get(5).getName());
	}

	private Response response(String json) {
		Request request = new Request.Builder().url("https://example.nexus.com").build();
		ResponseBody body = ResponseBody.create(MediaType.parse("text/json"), json);
		return new Response.Builder().request(request).protocol(Protocol.HTTP_2).body(body).code(200).message("OK")
				.build();

	}

	private String item(String tag) {
		return "{\"path\": \"v2/COMP_NAME/manifests/" + tag + "\"}";
	}

}
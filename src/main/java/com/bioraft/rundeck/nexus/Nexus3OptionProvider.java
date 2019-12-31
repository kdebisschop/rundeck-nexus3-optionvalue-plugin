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

import java.util.List;
import java.util.Map;

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

	@PluginProperty(title = "Endpoint host", description = "Nexus server hostname", required = false, defaultValue = "", scope = PropertyScope.Project)
	private String endpointHost;

	@PluginProperty(title = "Endpoint path", description = "Nexus path with leading slash", required = true, defaultValue = "/service/rest/v1/search/assets", scope = PropertyScope.Project)
	private String endpointPath;

	@PluginProperty(title = "User", description = "Nexus server user name", required = false, defaultValue = "", scope = PropertyScope.Project)
	private String user;
	
	@PluginProperty(title = "Password", description = "Nexus server password", required = false, defaultValue = "", scope = PropertyScope.Project)
	private String password;
	
	@PluginProperty(title = "Repository", description = "Nexus repository", required = true, defaultValue = "docker", scope = PropertyScope.Project)
	private String repository;

	@PluginProperty(title = "Component name", description = "Nexus component name", required = true, defaultValue = "*", scope = PropertyScope.Project)
	private String componentName;

	@PluginProperty(title = "Component version", description = "Nexus component version", required = false, scope = PropertyScope.Project)
	private String componentVersion;

	public Nexus3OptionProvider() {
		this.client = new OkHttpClient();
	}

	public Nexus3OptionProvider(OkHttpClient client) {
		this.client = client;
	}

	@Override
	public List<OptionValue> getOptionValues(Map configuration) {
		@SuppressWarnings("unchecked")
		Map<String, String> config = (Map<String, String>) configuration;

		if (!config.containsKey("endpointScheme") && endpointScheme != null && endpointScheme.length() > 0) {
			config.put("endpointScheme", endpointScheme);
		}

		if (!config.containsKey("endpointHost") && endpointHost != null && endpointHost.length() > 0) {
			config.put("endpointHost", endpointHost);
		}

		if (!config.containsKey("endpointPath") && endpointPath != null && endpointPath.length() > 0) {
			config.put("endpointPath", endpointPath);
		}

		if (!config.containsKey("user") && user != null && user.length() > 0) {
			config.put("user", user);
		}

		if (!config.containsKey("password") && password != null && password.length() > 0) {
			config.put("password", password);
		}

		if (!config.containsKey("repository") && repository != null && repository.length() > 0) {
			config.put("repository", repository);
		}

		if (!config.containsKey("componentName") && componentName != null && componentName.length() > 0) {
			config.put("componentName", componentName);
		}

		if (!config.containsKey("componentVersion") && componentVersion != null && componentVersion.length() > 0) {
			config.put("componentVersion", componentVersion);
		}

		OptionProviderImpl worker = new OptionProviderImpl(client);
		return worker.getOptionValues(config);
	}
}
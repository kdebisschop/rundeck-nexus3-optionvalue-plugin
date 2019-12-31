# Rundeck Nexus 3 Option Values Plugin

This plugin provides option values from a Nexus repository.

## Nexus 3 Option Values

The plugin connects to a remote Nexus API and returns the list of images
contained in that repository, filtered to show only the most recent build
for each image.

It assumes images are either releases that have a recognizable semantic
version tag or are branches. The branches or releases are suffixed by some
sort of build designator. Build designators are the last part of the image
tag and are separated by one of "_", "-", or "+".

## Configuration

Plugin settings are configured in framework.properties and/or project.properties.

You must configure endpointHost. One of:

 - framework.plugin.OptionValues.Nexus3OptionProvider.endpointHost=value
 - project.plugin.OptionValues.Nexus3OptionProvider.endpointHost=value

You will generally want to configure componentName. One of:

 - framework.plugin.OptionValues.Nexus3OptionProvider.componentName=value
 - project.plugin.OptionValues.Nexus3OptionProvider.componentName=value
 
If credentials are configured, the plugin will authenticate to the Nexus server.
 
 The full set of parameters is:
 
 - endpointScheme (default: https)
 - endpointHost (no default)
 - endpointPath (default: /service/rest/v1/search/assets)
 - user (no default)
 - password (no default)
 - repository (default: docker)
 - componentName (default: *)
 - componentVersion (default:*)

## Extending

You can easily create additional OptionValue plugins:

```
ipmort ...

@Plugin(name = ComponentOptionProvider.PLUGIN_NAME, service = ServiceNameConstants.OptionValues)
@PluginDescription(title = "MyComponent Images", description = "Filtered and sorted images on nexus server.")
public class ComponentOptionProvider implements OptionValuesPlugin {
  
	public static final String PLUGIN_NAME = "ComponentOptionProvider";

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

	@Override
	public List<OptionValue> getOptionValues(@SuppressWarnings("rawtypes") Map configuration) {
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

		config.put("componentName", "MyComponent");

		OptionProviderImpl worker = new OptionProviderImpl();
		return worker.getOptionValues(config);
	}
}
 ```
 
 This will allow you to configure OptionValue plugins for multiple components within a single Rundeck project.
 
## Known Issues

 - It should be possible to suppress filtering
 - Only one configuration is possible in a given project (this is a RunDeck limitation)
 
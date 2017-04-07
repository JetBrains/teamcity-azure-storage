# TeamCity Azure Storage

<a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityAzureStorageBuild&guest=1"><img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityAzureStorageBuild)/statusIcon.svg" alt=""/></a>

TeamCity Azure Storage Plugin is an implementation of external storage for TeamCity [build artifacts](https://confluence.jetbrains.com/display/TCDL/Build+Artifact) in Azure Blob Storage.
 
# Features

When installed and configured, the plugin:
* allows uploading artifacts to Azure Blob Storage
* allows downloading artifacts from Azure Blob Storage
* handles resolution of artifact dependencies
* handles clean-up of artifacts 
* displays artifacts located in Azure Blob Storage in the TeamCity UI.
 
# Download

You can [download plugin](https://plugins.jetbrains.com/teamcity) and install it as [an additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).

# Compatibility

The plugin is compatible with [TeamCity](https://www.jetbrains.com/teamcity/download/) 2017.1 and greater.

# Configuring 

The plugin adds the Artifacts Storage tab to the Project Settings page in the TeamCity Web UI. 
The tab lists the internal TeamCity artifacts storage is displayed by default and is marked as active.

To configure Azure Blob Storage for TeamCity artifacts, perform the following:
1. Select Azure Storage as the storage type
2. Fill in account name and key
3. Save your settings.

The configured Azure storage will appear on the Artifacts storage page. Make it active using the corresponding link.
Now the artifacts of this project, its subprojects and build configurations will be stored in the configured storage.

# Build

This project uses gradle as a build system. You can easily open it in [IntelliJ IDEA](https://www.jetbrains.com/idea/help/importing-project-from-gradle-model.html) or [Eclipse](http://gradle.org/eclipse/).
To test & build plugin you need to execute `build` gradle command.

# Contributions

We appreciate all kinds of feedback, so please feel free to send a PR or write an issue.

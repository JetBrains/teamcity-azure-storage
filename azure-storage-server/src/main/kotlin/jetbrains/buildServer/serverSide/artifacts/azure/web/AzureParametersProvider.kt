

package jetbrains.buildServer.serverSide.artifacts.azure.web

import jetbrains.buildServer.parameters.ReferencesResolverUtil
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants

class AzureParametersProvider {
    val accountName: String
        get() = AzureConstants.PARAM_ACCOUNT_NAME
    val accountKey: String
        get() = AzureConstants.PARAM_ACCOUNT_KEY
    val containerName: String
        get() = AzureConstants.PARAM_CONTAINER_NAME
    val containersPath: String
        get() = "/plugins/${AzureConstants.STORAGE_TYPE}/${AzureConstants.SETTINGS_PATH}.html"
    val pathPrefix: String
        get() = ReferencesResolverUtil.makeReference(AzureConstants.PATH_PREFIX_SYSTEM_PROPERTY)
}
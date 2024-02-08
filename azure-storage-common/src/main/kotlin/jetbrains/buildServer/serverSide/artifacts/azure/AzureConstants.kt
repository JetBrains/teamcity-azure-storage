

package jetbrains.buildServer.serverSide.artifacts.azure

import jetbrains.buildServer.agent.Constants

object AzureConstants {
    const val STORAGE_TYPE = "azure-storage"
    const val SETTINGS_PATH = "azure_storage_settings"
    const val PARAM_ACCOUNT_NAME = "account-name"
    const val PARAM_ACCOUNT_KEY = Constants.SECURE_PROPERTY_PREFIX + "account-key"
    const val PARAM_CONTAINER_NAME = "container-name"
    const val PATH_PREFIX_ATTR = "azure_path_prefix"
    const val PATH_PREFIX_SYSTEM_PROPERTY = "storage.azure.path.prefix"
    const val URL_LIFETIME_SEC = "storage.azure.url.expiration.time.seconds"
    const val DEFAULT_URL_LIFETIME_SEC = 120
    const val URL_HTTP_CACHE_MAX_AGE_SEC = "storage.azure.url.http.cache.max.age.seconds"
    const val DEFAULT_URL_HTTP_CACHE_MAX_AGE_SEC = 60
    const val WRITE_BUFFER_SIZE_PROPERTY = "storage.azure.write.buffer.size"
    const val DEFAULT_WRITE_BUFFER_SIZE = 0x40_0000
    const val WRITE_CONCURRENT_REQUEST_COUNT_PROPERTY = "storage.azure.write.concurrent.request.count"
}
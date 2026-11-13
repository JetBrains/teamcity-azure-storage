

package jetbrains.buildServer.serverSide.artifacts.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.common.StorageSharedKeyCredential
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.util.FileUtil
import java.io.File
import java.lang.reflect.Method
import java.net.URLConnection
import java.net.UnknownHostException

object AzureUtils {
    /**
     * Gets a list of azure storage parameters.
     */
    fun getParameters(parameters: Map<String, String>): Map<String, String> {
        val result = hashMapOf<String, String>()

        parameters[AzureConstants.PARAM_ACCOUNT_NAME]?.trim()?.let {
            result[AzureConstants.PARAM_ACCOUNT_NAME] = it
        }

        parameters[AzureConstants.PARAM_ACCOUNT_KEY]?.trim()?.let {
            result[AzureConstants.PARAM_ACCOUNT_KEY] = it
        }

        parameters[AzureConstants.PARAM_CONTAINER_NAME]?.trim()?.let {
            result[AzureConstants.PARAM_CONTAINER_NAME] = it
        }

        return result
    }

    fun validateParameters(parameters: Map<String, String>): Boolean {
        return parameters.contains(AzureConstants.PARAM_ACCOUNT_NAME)
                && parameters.contains(AzureConstants.PARAM_ACCOUNT_KEY)
    }

    fun getPathPrefix(properties: Map<String, String>) = properties[AzureConstants.PATH_PREFIX_ATTR]

    fun getWriteBufferSize(): Long = TeamCityProperties.getInteger(AzureConstants.WRITE_BUFFER_SIZE_PROPERTY, AzureConstants.DEFAULT_WRITE_BUFFER_SIZE).toLong()

    fun getWriteConcurrentRequestCount(): Int {
        val defaultThreadCount = Runtime.getRuntime().availableProcessors()
        return TeamCityProperties.getInteger(AzureConstants.WRITE_CONCURRENT_REQUEST_COUNT_PROPERTY, defaultThreadCount)
    }

    fun getTimeoutIntervalInMs(): Int = TeamCityProperties.getInteger(AzureConstants.TIMEOUT_INTERVAL_PROPERTY, AzureConstants.DEFAULT_TIMEOUT_INTERVAL_IN_MS)

    fun getArtifactPath(properties: Map<String, String>, path: String): String {
        return getPathPrefix(properties) + FORWARD_SLASH + path
    }

    fun getBlobClient(parameters: Map<String, String>): BlobServiceClient {
        val accountName = parameters[AzureConstants.PARAM_ACCOUNT_NAME]?.trim()
        val accountKey = parameters[AzureConstants.PARAM_ACCOUNT_KEY]?.trim()
        val credential = StorageSharedKeyCredential(accountName, accountKey)
        val endpoint = "https://${accountName}.blob.core.windows.net"
        return BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient()
    }

    fun getBlobReference(parameters: Map<String, String>, path: String): BlobClient {
        val serviceClient = getBlobClient(parameters)
        val (containerName, blobPath) = getContainerAndPath(path)
                ?: throw IllegalArgumentException("Path should not be empty")
        val containerClient = serviceClient.getBlobContainerClient(containerName)
        return containerClient.getBlobClient(blobPath)
    }

    fun getContainerAndPath(pathPrefix: String): Pair<String, String>? {
        val pathSegments = pathPrefix.split(FORWARD_SLASH).filter { it.isNotEmpty() }
        if (pathSegments.isEmpty()) return null
        return pathSegments.first() to pathSegments
                .takeLast(pathSegments.size - 1)
                .joinToString("$FORWARD_SLASH")
                .trimStart(FORWARD_SLASH)
    }

    fun appendPathPrefix(pathPrefix: String, fileName: String): String {
        val path = pathPrefix.trimEnd(FORWARD_SLASH)
        return if (path.isEmpty()) {
            fileName
        } else {
            FileUtil.normalizeRelativePath("$path$FORWARD_SLASH$fileName")
        }
    }

    fun getExceptionMessage(exception: Throwable): String {
        val e = if (exception is NoSuchElementException)
            exception.cause ?: exception else exception
        return when (e) {
            is BlobStorageException -> {
                if (e.cause is UnknownHostException) {
                    "Invalid account name: ${e.cause?.message}"
                } else {
                    "Invalid account key"
                }
            }
            is StringIndexOutOfBoundsException,
            is IllegalArgumentException -> {
                "Invalid account key"
            }
            else -> {
                e.message ?: e.toString()
            }
        }
    }

    fun getContentType(file: File): String {
        URLConnection.guessContentTypeFromName(file.name)?.let {
            return it
        }
        if (probeContentTypeMethod != null && fileToPathMethod != null) {
            try {
                probeContentTypeMethod.invoke(null, fileToPathMethod.invoke(file))?.let {
                    if (it is String) {
                        return it
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        return DEFAULT_CONTENT_TYPE
    }

    private fun getProbeContentTypeMethod(): Method? {
        try {
            val filesClass = Class.forName("java.nio.file.Files")
            val pathClass = Class.forName("java.nio.file.Path")
            if (filesClass != null && pathClass != null) {
                return filesClass.getMethod("probeContentType", pathClass)
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun getFileToPathMethod(): Method? {
        try {
            return File::class.java.getMethod("toPath")
        } catch (ignored: Exception) {
        }
        return null
    }

    private const val FORWARD_SLASH = '/'
    private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    private val probeContentTypeMethod: Method? = getProbeContentTypeMethod()
    private val fileToPathMethod: Method? = getFileToPathMethod()
}
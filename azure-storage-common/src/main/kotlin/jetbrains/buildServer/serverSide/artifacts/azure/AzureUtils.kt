/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.serverSide.artifacts.azure

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlockBlob
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

    fun getPathPrefix(properties: Map<String, String>) = properties[AzureConstants.PATH_PREFIX_ATTR]

    fun getArtifactPath(properties: Map<String, String>, path: String): String {
        return getPathPrefix(properties) + FORWARD_SLASH + path
    }

    fun getBlobClient(parameters: Map<String, String>): CloudBlobClient {
        val accountName = parameters[AzureConstants.PARAM_ACCOUNT_NAME]?.trim()
        val accountKey = parameters[AzureConstants.PARAM_ACCOUNT_KEY]?.trim()
        return CloudStorageAccount(StorageCredentialsAccountAndKey(accountName, accountKey)).createCloudBlobClient()
    }

    fun getBlobReference(parameters: Map<String, String>, path: String): CloudBlockBlob {
        val client = AzureUtils.getBlobClient(parameters)
        val pathSegments = path.split(FORWARD_SLASH)
        val container = client.getContainerReference(pathSegments.first())
        val blobPath = pathSegments.takeLast(pathSegments.size - 1).joinToString("$FORWARD_SLASH")
        return container.getBlockBlobReference(blobPath)
    }

    fun getContainerAndPath(pathPrefix: String): Pair<String, String>? {
        val pathSegments = pathPrefix.split(FORWARD_SLASH)
        if (pathSegments.isEmpty()) return null
        return pathSegments.first() to pathSegments
                .takeLast(pathSegments.size - 1)
                .joinToString("$FORWARD_SLASH", postfix = "$FORWARD_SLASH")
    }

    fun getExceptionMessage(exception: Throwable): String {
        val e = if (exception is NoSuchElementException)
            exception.cause ?: exception else exception
        return when (e) {
            is StorageException -> {
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

    const val FORWARD_SLASH = '/'
}
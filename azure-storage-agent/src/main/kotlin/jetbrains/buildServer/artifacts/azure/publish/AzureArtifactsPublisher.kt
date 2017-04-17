/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.artifacts.azure.publish

import com.intellij.openapi.diagnostic.Logger
import com.microsoft.azure.storage.StorageException
import jetbrains.buildServer.ArtifactsConstants
import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.artifacts.AgentArtifactHelper
import jetbrains.buildServer.artifacts.ArtifactDataInstance
import jetbrains.buildServer.log.LogUtil
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants.PATH_PREFIX_ATTR
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants.PATH_PREFIX_SYSTEM_PROPERTY
import jetbrains.buildServer.serverSide.artifacts.azure.AzureUtils
import jetbrains.buildServer.util.EventDispatcher
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI

class AzureArtifactsPublisher(dispatcher: EventDispatcher<AgentLifeCycleListener>,
                              private val helper: AgentArtifactHelper,
                              private val tracker: CurrentBuildTracker)
    : ArtifactsPublisher {

    private val publishedArtifacts = arrayListOf<ArtifactDataInstance>()

    init {
        dispatcher.addListener(object : AgentLifeCycleAdapter() {
            override fun buildStarted(build: AgentRunningBuild) {
                publishedArtifacts.clear()
            }

            override fun afterAtrifactsPublished(build: AgentRunningBuild, status: BuildFinishedStatus) {
                publishArtifactsList(build)
            }
        })
    }

    override fun publishFiles(filePathMap: Map<File, String>): Int {
        val filesToPublish = filePathMap.entries.filter {
            !it.value.startsWith(ArtifactsConstants.TEAMCITY_ARTIFACTS_DIR)
        }

        if (filesToPublish.isNotEmpty()) {
            val build = tracker.currentBuild
            try {
                val parameters = publisherParameters

                if (publishedArtifacts.isEmpty()) {
                    setPathPrefixProperty(build)
                }

                val client = AzureUtils.getBlobClient(parameters)
                val pathValue = getPathPrefixProperty(build)
                val (containerName, pathPrefix) = AzureUtils.getContainerAndPath(pathValue) ?:
                        throw ArtifactPublishingFailedException("Invalid $PATH_PREFIX_SYSTEM_PROPERTY build system property", false, null)

                val container = client.getContainerReference(containerName)
                if (publishedArtifacts.isEmpty()) {
                    container.createIfNotExists()
                }

                filesToPublish.forEach { (file, path) ->
                    val filePath = preparePath(path, file)
                    val blobName = pathPrefix + filePath
                    val blob = container.getBlockBlobReference(blobName)

                    FileInputStream(file).use {
                        val length = file.length()
                        blob.upload(it, length)
                        val artifact = ArtifactDataInstance.create(filePath, length)
                        publishedArtifacts.add(artifact)
                    }
                }
            } catch (e: Throwable) {
                val message = "Failed to publish files"
                LOG.warnAndDebugDetails(message, e)

                if (e is StorageException) {
                    e.extendedErrorInformation?.errorMessage?.let {
                        LOG.warn(it)
                        build.buildLogger.error(it)
                    }
                }

                throw ArtifactPublishingFailedException("$message: ${e.message}", false, e)
            }
        }

        return filesToPublish.size
    }

    private fun preparePath(path: String, file: File): String {
        if (path.startsWith(".."))
            throw IOException("Attempting to publish artifact outside of build artifacts directory. Specified target path: \"$path\"")

        return if (path.isEmpty()) {
            file.name
        } else {
            URI("$path$SLASH${file.name}").normalize().path
        }
    }

    override fun getType() = AzureConstants.STORAGE_TYPE

    override fun isEnabled() = true

    private val publisherParameters get() = AzureUtils.getParameters(tracker.currentBuild.artifactStorageSettings)

    private fun publishArtifactsList(build: AgentRunningBuild) {
        if (publishedArtifacts.isNotEmpty()) {
            val pathPrefix = getPathPrefixProperty(build)
            val properties = mapOf(PATH_PREFIX_ATTR to pathPrefix)
            try {
                helper.publishArtifactList(publishedArtifacts, properties)
            } catch (e: IOException) {
                build.buildLogger.error(ERROR_PUBLISHING_ARTIFACTS_LIST + ": " + e.message)
                LOG.warnAndDebugDetails(ERROR_PUBLISHING_ARTIFACTS_LIST + "for build " + LogUtil.describe(build), e)
            }
        }
    }

    private fun setPathPrefixProperty(build: AgentRunningBuild) {
        val pathPrefix = getPathPrefix(build)
        build.addSharedSystemProperty(PATH_PREFIX_SYSTEM_PROPERTY, pathPrefix)
    }

    private fun getPathPrefixProperty(build: AgentRunningBuild): String {
        return build.sharedBuildParameters.systemProperties[PATH_PREFIX_SYSTEM_PROPERTY] ?:
                throw ArtifactPublishingFailedException("No $PATH_PREFIX_SYSTEM_PROPERTY build system property found", false, null)
    }

    /**
     * Calculates path prefix.
     */
    private fun getPathPrefix(build: AgentRunningBuild): String {
        // Try to get overriden path prefix
        val pathSegments = (build.sharedConfigParameters[PATH_PREFIX_SYSTEM_PROPERTY] ?: "")
                .trim()
                .replace('\\', SLASH)
                .split(SLASH)
                .filter { it.isNotEmpty() }
                .toMutableList()

        // Set default path prefix
        if (pathSegments.isEmpty()) {
            build.sharedConfigParameters[ServerProvidedProperties.TEAMCITY_PROJECT_ID_PARAM]?.let {
                pathSegments.add(it)
            }
            pathSegments.add(build.buildTypeExternalId)
            pathSegments.add(build.buildId.toString())
        }

        // Add container name if specified
        val parameters = publisherParameters
        parameters[AzureConstants.PARAM_CONTAINER_NAME]?.let {
            pathSegments.add(0, it)
        }

        // Sanitize container name: length < 64, lowercase, alphanumeric and dash
        val firstSegment = CONTAINER_INVALID_CHARS_REGEX.replace(pathSegments.first(), "")
        val containerName = firstSegment.substring(0, Math.min(firstSegment.length, 63)).toLowerCase()

        return pathSegments
                .takeLast(pathSegments.size - 1)
                .joinToString("$SLASH", prefix = "$containerName$SLASH")
    }

    companion object {
        private val LOG = Logger.getInstance(AzureArtifactsPublisher::class.java.name)
        private val ERROR_PUBLISHING_ARTIFACTS_LIST = "Error publishing artifacts list"
        private val CONTAINER_INVALID_CHARS_REGEX = Regex("[^a-z0-9-]", RegexOption.IGNORE_CASE)
        private val SLASH = '/'
    }
}

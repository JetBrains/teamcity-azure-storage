/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.artifacts.azure.publish

import com.intellij.openapi.diagnostic.Logger
import com.microsoft.azure.storage.blob.BlobRequestOptions
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
                    val filePath = AzureUtils.appendPathPrefix(path, file.name)
                    val blobName = AzureUtils.appendPathPrefix(pathPrefix, filePath)
                    val blob = container.getBlockBlobReference(blobName)
                    blob.properties.contentType = AzureUtils.getContentType(file)

                    FileInputStream(file).use {
                        val length = file.length()
                        blob.upload(it, length, null, BlobRequestOptions().apply {
                            this.concurrentRequestCount = 8
                            this.timeoutIntervalInMs = 30_000
                        }, null)
                        val artifact = ArtifactDataInstance.create(filePath, length)
                        publishedArtifacts.add(artifact)
                    }
                }
            } catch (e: Throwable) {
                val message = AzureUtils.getExceptionMessage(e)

                LOG.warnAndDebugDetails(message, e)
                build.buildLogger.error(message)

                throw ArtifactPublishingFailedException(message, false, e)
            }
            publishArtifactsList(build)
        }

        return filesToPublish.size
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
        val pathSegments = arrayListOf<String>()

        // Try to get overriden path prefix
        val pathPrefix = build.sharedConfigParameters[PATH_PREFIX_SYSTEM_PROPERTY]
        if (pathPrefix == null) {
            // Set default path prefix
            build.sharedConfigParameters[ServerProvidedProperties.TEAMCITY_PROJECT_ID_PARAM]?.let {
                pathSegments.add(it)
            }
            pathSegments.add(build.buildTypeExternalId)
            pathSegments.add(build.buildId.toString())
        } else {
            pathSegments.addAll(pathPrefix
                    .trim()
                    .replace('\\', SLASH)
                    .split(SLASH)
                    .filter { it.isNotEmpty() })
        }

        // Add container name if specified
        val parameters = publisherParameters
        parameters[AzureConstants.PARAM_CONTAINER_NAME]?.let {
            pathSegments.add(0, it)
        }

        // Container name must be specified
        if (pathSegments.isEmpty()) {
            build.sharedConfigParameters[ServerProvidedProperties.TEAMCITY_PROJECT_ID_PARAM]?.let {
                pathSegments.add(it)
            }
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

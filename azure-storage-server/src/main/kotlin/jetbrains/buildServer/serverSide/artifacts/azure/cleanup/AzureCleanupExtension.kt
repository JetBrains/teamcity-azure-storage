/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.serverSide.artifacts.azure.cleanup

import jetbrains.buildServer.artifacts.ArtifactListData
import jetbrains.buildServer.artifacts.ServerArtifactStorageSettingsProvider
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.artifacts.ServerArtifactHelper
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants
import jetbrains.buildServer.serverSide.artifacts.azure.AzureUtils
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContextEx
import jetbrains.buildServer.serverSide.cleanup.CleanupExtension
import jetbrains.buildServer.serverSide.cleanup.CleanupProcessState
import jetbrains.buildServer.serverSide.impl.cleanup.HistoryRetentionPolicy
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.positioning.PositionAware
import jetbrains.buildServer.util.positioning.PositionConstraint

class AzureCleanupExtension(private val helper: ServerArtifactHelper,
                            private val settingsProvider: ServerArtifactStorageSettingsProvider)
    : CleanupExtension, PositionAware {

    override fun getOrderId() = AzureConstants.STORAGE_TYPE

    override fun cleanupBuildsData(cleanupContext: BuildCleanupContext) {
        for (build in cleanupContext.builds) {
            val artifactsInfo = helper.getArtifactList(build) ?: continue
            val pathPrefix = AzureUtils.getPathPrefix(artifactsInfo.commonProperties) ?: continue
            val (containerName, path) = AzureUtils.getContainerAndPath(pathPrefix) ?: continue

            val patterns = getPatternsForBuild(cleanupContext as BuildCleanupContextEx, build)
            val toDelete = getPathsToDelete(artifactsInfo, patterns)
            if (toDelete.isEmpty()) continue

            val parameters = settingsProvider.getStorageSettings(build)
            val client = AzureUtils.getBlobClient(parameters)

            val container = client.getContainerReference(containerName)
            val blobs = toDelete.map {
                container.getBlockBlobReference(path + it)
            }

            var succeededNum = 0
            blobs.forEach {
                try {
                    it.delete()
                    succeededNum++
                } catch (e: Throwable) {
                    Loggers.CLEANUP.debug("Failed to remove ${it.uri} from Azure storage: ${e.message}")
                }
            }

            val suffix = " from account [${client.credentials.accountName}] from path [$pathPrefix]"
            Loggers.CLEANUP.info("Removed [" + succeededNum + "] Azure storage " + StringUtil.pluralize("blob", succeededNum) + suffix)

            helper.removeFromArtifactList(build, toDelete)
        }
    }

    override fun afterCleanup(cleanupState: CleanupProcessState) {
    }

    override fun getConstraint() = PositionConstraint.first()

    private fun getPatternsForBuild(cleanupContext: BuildCleanupContextEx, build: SBuild): String {
        val policy = cleanupContext.getCleanupPolicyForBuild(build.buildId)
        return StringUtil.emptyIfNull(policy.parameters[HistoryRetentionPolicy.ARTIFACT_PATTERNS_PARAM])
    }

    private fun getPathsToDelete(artifactsInfo: ArtifactListData, patterns: String): List<String> {
        val keys = artifactsInfo.artifactList.map { it.path }
        return PathPatternFilter(patterns).filterPaths(keys)
    }
}
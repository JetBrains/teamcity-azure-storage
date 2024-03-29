

package jetbrains.buildServer.serverSide.artifacts.azure.cleanup

import jetbrains.buildServer.artifacts.ServerArtifactStorageSettingsProvider
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.artifacts.ServerArtifactHelper
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants
import jetbrains.buildServer.serverSide.artifacts.azure.AzureUtils
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContextEx
import jetbrains.buildServer.serverSide.cleanup.CleanupExtension
import jetbrains.buildServer.serverSide.cleanup.CleanupProcessState
import jetbrains.buildServer.serverSide.impl.cleanup.ArtifactPathsEvaluator
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

            val pathsToDelete = ArtifactPathsEvaluator.getPathsToDelete(cleanupContext as BuildCleanupContextEx, build, artifactsInfo)
            if (pathsToDelete.isEmpty()) continue

            val parameters = settingsProvider.getStorageSettings(build)

            if (AzureUtils.validateParameters(parameters)) {
                val client = AzureUtils.getBlobClient(parameters)

                val container = client.getContainerReference(containerName)
                val blobs = pathsToDelete.map {
                    val blobPath = AzureUtils.appendPathPrefix(path, it)
                    container.getBlockBlobReference(blobPath)
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
                Loggers.CLEANUP.info("Removed [" + succeededNum + "] Azure storage " + StringUtil.pluralize("blob",
                    succeededNum) + suffix)
            } else {
                Loggers.CLEANUP.warn("Cannot find Azure storage parameters for build id=${build.buildId}. [${pathsToDelete.size}] build artifacts at [$path] cannot be deleted")
            }
            helper.removeFromArtifactList(build, pathsToDelete)
        }
    }

    override fun afterCleanup(cleanupState: CleanupProcessState) {
    }

    override fun getConstraint() = PositionConstraint.first()

}
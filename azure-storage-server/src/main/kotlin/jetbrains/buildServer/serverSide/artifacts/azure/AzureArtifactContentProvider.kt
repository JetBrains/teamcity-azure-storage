

package jetbrains.buildServer.serverSide.artifacts.azure

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.artifacts.ArtifactContentProvider
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo
import java.io.IOException
import java.io.InputStream

class AzureArtifactContentProvider : ArtifactContentProvider {

    override fun getContent(artifactInfo: StoredBuildArtifactInfo): InputStream {
        val artifactData = artifactInfo.artifactData ?: throw IOException("Can not process artifact download request for a folder")

        val parameters = AzureUtils.getParameters(artifactInfo.storageSettings)
        val path = AzureUtils.getArtifactPath(artifactInfo.commonProperties, artifactData.path)

        try {
            val blob = AzureUtils.getBlobReference(parameters, path)
            return blob.openInputStream()
        } catch (e: Throwable) {
            val message = "Failed to get artifact $path from Azure Blob Storage"
            LOG.warnAndDebugDetails(message, e)
            throw IOException("$message: $e.message", e)
        }
    }

    override fun getType() = AzureConstants.STORAGE_TYPE

    companion object {
        private val LOG = Logger.getInstance(AzureArtifactContentProvider::class.java.name)
    }
}
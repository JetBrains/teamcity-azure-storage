/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.serverSide.artifacts.azure.web

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.diagnostic.Logger
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants
import jetbrains.buildServer.serverSide.artifacts.azure.AzureUtils
import jetbrains.buildServer.web.openapi.artifacts.ArtifactDownloadProcessor
import org.springframework.http.HttpHeaders
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AzureArtifactDownloadProcessor : ArtifactDownloadProcessor {
    private val myLinksCache = CacheBuilder.newBuilder()
            .expireAfterWrite(urlLifeTime.toLong(), TimeUnit.SECONDS)
            .maximumSize(100)
            .build<String, String>()

    override fun processDownload(artifactInfo: StoredBuildArtifactInfo,
                                 buildPromotion: BuildPromotion,
                                 request: HttpServletRequest,
                                 response: HttpServletResponse): Boolean {
        val artifactData = artifactInfo.artifactData ?: throw IOException("Can not process artifact download request for a folder")

        val params = AzureUtils.getParameters(artifactInfo.storageSettings)
        val path = AzureUtils.getArtifactPath(artifactInfo.commonProperties, artifactData.path)
        val lifeTime = urlLifeTime

        val temporaryUrl = getTemporaryUrl(path, params, lifeTime * 2)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + lifeTime)
        response.sendRedirect(temporaryUrl)

        return true
    }

    override fun getType() = AzureConstants.STORAGE_TYPE

    private val urlLifeTime
            get() = TeamCityProperties.getInteger(AzureConstants.URL_LIFETIME_SEC, AzureConstants.DEFAULT_URL_LIFETIME_SEC)

    private fun getTemporaryUrl(path: String, parameters: Map<String, String>, lifeTime: Int): String {
        try {
            return myLinksCache.get(getIdentity(parameters, path), {
                val blob = AzureUtils.getBlobReference(parameters, path)
                val policy = createSharedAccessPolicy(lifeTime)
                val sasToken = blob.generateSharedAccessSignature(policy, null)
                val credentials = StorageCredentialsSharedAccessSignature(sasToken)
                credentials.transformUri(blob.uri).toString()
            })
        } catch (e: Throwable) {
            val message = "Failed to create URl for blob $path"
            LOG.infoAndDebugDetails(message, e)
            throw IOException(message + ": " + e.message, e)
        }
    }

    private fun getIdentity(params: Map<String, String>, path: String): String {
        return StringBuilder().apply {
            append(params[AzureConstants.PARAM_ACCOUNT_NAME] ?: "")
            append(params[AzureConstants.PARAM_ACCOUNT_KEY] ?: "")
            append(params[AzureConstants.PARAM_CONTAINER_NAME] ?: "")
            append(path)
        }.toString().toLowerCase().hashCode().toString()
    }

    private fun createSharedAccessPolicy(expireTimeInSeconds: Int): SharedAccessBlobPolicy {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        cal.time = Date()
        cal.add(Calendar.SECOND, expireTimeInSeconds)
        val policy = SharedAccessBlobPolicy()
        policy.permissions = EnumSet.of(SharedAccessBlobPermissions.READ)
        policy.sharedAccessExpiryTime = cal.time
        return policy
    }

    companion object {
        private val LOG = Logger.getInstance(AzureArtifactDownloadProcessor::class.java.name)
    }
}
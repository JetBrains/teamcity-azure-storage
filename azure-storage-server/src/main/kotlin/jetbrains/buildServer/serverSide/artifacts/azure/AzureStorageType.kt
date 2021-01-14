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

package jetbrains.buildServer.serverSide.artifacts.azure

import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageType
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageTypeRegistry
import jetbrains.buildServer.web.openapi.PluginDescriptor

class AzureStorageType(registry: ArtifactStorageTypeRegistry,
                       private val descriptor: PluginDescriptor) : ArtifactStorageType() {
    private val EMPTY_VALUE = "Should not be empty"

    init {
        registry.registerStorageType(this)
    }

    override fun getName() = "Azure Storage"

    override fun getDescription() = "Provides Azure storage support for TeamCity artifacts"

    override fun getType() = AzureConstants.STORAGE_TYPE

    override fun getEditStorageParametersPath() = descriptor.getPluginResourcesPath(AzureConstants.SETTINGS_PATH + ".jsp")

    override fun getParametersProcessor(): PropertiesProcessor? {
        return PropertiesProcessor {
            val invalidProperties = arrayListOf<InvalidProperty>()
            val parameters = hashMapOf<String, String>()

            AzureConstants.PARAM_ACCOUNT_NAME.apply {
                if (it[this].isNullOrEmpty()) {
                    invalidProperties.add(InvalidProperty(this, EMPTY_VALUE))
                } else {
                    parameters[this] = it[this]!!
                }
            }

            AzureConstants.PARAM_ACCOUNT_KEY.apply {
                if (it[this].isNullOrEmpty()) {
                    invalidProperties.add(InvalidProperty(this, EMPTY_VALUE))
                } else {
                    parameters[this] = it[this]!!
                }
            }

            if (parameters.size == 2) {
                try {
                    AzureUtils.getBlobClient(parameters).downloadServiceProperties()
                } catch (e: Throwable) {
                    val message = AzureUtils.getExceptionMessage(e)
                    invalidProperties.add(InvalidProperty(AzureConstants.PARAM_ACCOUNT_KEY, message))
                }
            }

            invalidProperties
        }
    }
}
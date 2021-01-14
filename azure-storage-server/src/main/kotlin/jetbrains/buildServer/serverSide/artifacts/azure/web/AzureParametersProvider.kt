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

import jetbrains.buildServer.parameters.ReferencesResolverUtil
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants

class AzureParametersProvider {
    val accountName: String
        get() = AzureConstants.PARAM_ACCOUNT_NAME
    val accountKey: String
        get() = AzureConstants.PARAM_ACCOUNT_KEY
    val containerName: String
        get() = AzureConstants.PARAM_CONTAINER_NAME
    val containersPath: String
        get() = "/plugins/${AzureConstants.STORAGE_TYPE}/${AzureConstants.SETTINGS_PATH}.html"
    val pathPrefix: String
        get() = ReferencesResolverUtil.makeReference(AzureConstants.PATH_PREFIX_SYSTEM_PROPERTY)
}
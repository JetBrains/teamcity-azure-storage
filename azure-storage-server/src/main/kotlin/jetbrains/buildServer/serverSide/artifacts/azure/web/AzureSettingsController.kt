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

import jetbrains.buildServer.controllers.ActionErrors
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants
import jetbrains.buildServer.serverSide.artifacts.azure.AzureUtils
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AzureSettingsController(server: SBuildServer,
                              manager: WebControllerManager,
                              descriptor: PluginDescriptor)
    : BaseFormXmlController(server) {

    init {
        manager.registerController(descriptor.getPluginResourcesPath(AzureConstants.SETTINGS_PATH + ".html"), this)
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) = null

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        val errors = ActionErrors()
        val parameters = getProperties(request)

        try {
            val client = AzureUtils.getBlobClient(parameters)
            val containers = client.listContainers().toList()
            val containersElement = Element("containers")
            containers.forEach {
                containersElement.addContent(Element("container").apply {
                    text = it.name
                })
            }
            xmlResponse.addContent(containersElement)
        } catch (e: Throwable) {
            val message = AzureUtils.getExceptionMessage(e)
            errors.addError(AzureConstants.PARAM_ACCOUNT_KEY, message)
        }

        if (errors.hasErrors()) {
            errors.serialize(xmlResponse)
        }
    }

    private fun getProperties(request: HttpServletRequest): Map<String, String> {
        val propsBean = BasePropertiesBean(null)
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
        return propsBean.properties
    }
}
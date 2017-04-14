/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
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
        val props = getProperties(request)
        val accountName = props[AzureConstants.PARAM_ACCOUNT_NAME] ?: ""
        val accountKey = props[AzureConstants.PARAM_ACCOUNT_KEY] ?: ""

        try {
            val parameters = hashMapOf(
                    AzureConstants.PARAM_ACCOUNT_NAME to accountName,
                    AzureConstants.PARAM_ACCOUNT_KEY to accountKey)
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
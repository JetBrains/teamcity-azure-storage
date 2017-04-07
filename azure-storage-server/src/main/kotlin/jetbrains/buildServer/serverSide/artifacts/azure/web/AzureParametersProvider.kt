/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.serverSide.artifacts.azure.web

import jetbrains.buildServer.serverSide.artifacts.azure.AzureConstants

class AzureParametersProvider {
    val accountName: String
        get() = AzureConstants.PARAM_ACCOUNT_NAME
    val accountKey: String
        get() = AzureConstants.PARAM_ACCOUNT_KEY
}
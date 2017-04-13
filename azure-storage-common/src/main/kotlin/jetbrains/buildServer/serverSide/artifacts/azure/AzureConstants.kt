/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.serverSide.artifacts.azure

import jetbrains.buildServer.agent.Constants

object AzureConstants {
    const val STORAGE_TYPE = "azure-storage"
    const val SETTINGS_PATH = "azure_storage_settings"
    const val PARAM_ACCOUNT_NAME = "account-name"
    const val PARAM_ACCOUNT_KEY = Constants.SECURE_PROPERTY_PREFIX + "account-key"
    const val PARAM_CONTAINER_NAME = "container-name"
    const val PATH_PREFIX_ATTR = "azure_path_prefix"
    const val PATH_PREFIX_SYSTEM_PROPERTY = "storage.azure.path.prefix"
    const val URL_LIFETIME_SEC = "storage.azure.url.expiration.time.seconds"
    const val DEFAULT_URL_LIFETIME_SEC = 60
}
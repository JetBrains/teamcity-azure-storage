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

import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class AzureUtilsTest {
    @DataProvider
    fun getContentTypeData(): Array<Array<Any?>> {
        return arrayOf(
                arrayOf<Any?>("file.css", "text/css"),
                arrayOf<Any?>("file.zip", "application/zip"),
                arrayOf<Any?>("file.txt", "text/plain"),
                arrayOf<Any?>("file.jpg", "image/jpeg"),
                arrayOf<Any?>("file.bin", "application/octet-stream")
        )
    }

    @Test(dataProvider = "getContentTypeData")
    fun getContentTypeTest(fileName: String, expectedType: String) {
        Assert.assertEquals(AzureUtils.getContentType(File("AzureUtilsTest/$fileName")), expectedType)
    }
}
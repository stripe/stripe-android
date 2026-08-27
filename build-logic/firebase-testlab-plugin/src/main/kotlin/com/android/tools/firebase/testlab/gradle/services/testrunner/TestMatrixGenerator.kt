/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.firebase.testlab.gradle.services.testrunner

import com.android.build.api.instrumentation.StaticTestData
import com.android.tools.firebase.testlab.gradle.FixtureImpl
import com.android.tools.firebase.testlab.gradle.services.TestLabBuildService
import com.android.tools.firebase.testlab.gradle.services.storage.TestRunStorage
import com.android.tools.firebase.testlab.gradle.services.toUrl
import com.google.api.client.json.GenericJson
import com.google.api.client.util.Key
import com.google.api.services.storage.model.StorageObject
import com.google.api.services.testing.model.AndroidDevice
import com.google.api.services.testing.model.AndroidDeviceList
import com.google.api.services.testing.model.AndroidInstrumentationTest
import com.google.api.services.testing.model.ClientInfo
import com.google.api.services.testing.model.DeviceFile
import com.google.api.services.testing.model.EnvironmentMatrix
import com.google.api.services.testing.model.EnvironmentVariable
import com.google.api.services.testing.model.FileReference
import com.google.api.services.testing.model.GoogleCloudStorage
import com.google.api.services.testing.model.RegularFile
import com.google.api.services.testing.model.ResultStorage
import com.google.api.services.testing.model.ShardingOption
import com.google.api.services.testing.model.TestMatrix
import com.google.api.services.testing.model.TestSetup
import com.google.api.services.testing.model.TestSpecification
import com.google.api.services.testing.model.ToolResultsHistory
import com.google.api.services.testing.model.UniformSharding

class TestMatrixGenerator(private val projectSettings: ProjectSettings) {

    companion object {
        const val TEST_MATRIX_FLAKY_TEST_ATTEMPTS_FIELD = "flakyTestAttempts"
        const val TEST_MATRIX_FAIL_FAST_FIELD = "failFast"
    }

    private class SmartSharding : GenericJson() {
        @Key
        var targetedShardDuration: String? = null
    }

    fun createTestMatrix(
        device: TestDeviceData,
        testData: StaticTestData,
        testRunStorage: TestRunStorage,
        testApkObject: StorageObject,
        testedApkObject: StorageObject,
    ): TestMatrix = TestMatrix().apply {
        projectId = projectSettings.name
        clientInfo = ClientInfo().apply { name = TestLabBuildService.CLIENT_APPLICATION_NAME }
        testSpecification = TestSpecification().apply {
            testSetup = TestSetup().apply {
                set(
                    "dontAutograntPermissions",
                    projectSettings.grantedPermissions == FixtureImpl.GrantedPermissions.NONE.name,
                )
                projectSettings.networkProfile?.apply { networkProfile = this }
                filesToPush = mutableListOf()
                device.extraDeviceFileUrls.forEach { (onDevicePath, gcsUrl) ->
                    filesToPush.add(
                        DeviceFile().apply {
                            regularFile = RegularFile().apply {
                                content = FileReference().apply { gcsPath = gcsUrl }
                                devicePath = onDevicePath
                            }
                        }
                    )
                }
                directoriesToPull = projectSettings.directoriesToPull
                environmentVariables = testData.instrumentationRunnerArguments.map { entry ->
                    EnvironmentVariable().apply {
                        key = entry.key
                        value = entry.value
                    }
                }
            }
            androidInstrumentationTest = AndroidInstrumentationTest().apply {
                testApk = FileReference().apply { gcsPath = testApkObject.toUrl() }
                appApk = FileReference().apply { gcsPath = testedApkObject.toUrl() }
                appPackageId = testData.testedApplicationId
                testPackageId = testData.applicationId
                testRunnerClass = testData.instrumentationRunner
                if (projectSettings.useOrchestrator) {
                    orchestratorOption = "USE_ORCHESTRATOR"
                }
                shardingOption = createShardingOption()
            }
            testTimeout = "${projectSettings.ftlTimeoutSeconds}s"
            disablePerformanceMetrics = !projectSettings.performanceMetrics
            disableVideoRecording = !projectSettings.videoRecording
        }
        environmentMatrix = EnvironmentMatrix().apply {
            androidDeviceList = AndroidDeviceList().apply {
                androidDevices = listOf(
                    AndroidDevice().apply {
                        androidModelId = device.deviceId
                        androidVersionId = device.apiLevel.toString()
                        locale = device.locale.toString()
                        orientation = device.orientation.toString().lowercase()
                    }
                )
            }
        }
        resultStorage = ResultStorage().apply {
            googleCloudStorage = GoogleCloudStorage().apply {
                gcsPath = testRunStorage.resultStoragePath
            }
            toolResultsHistory = ToolResultsHistory().apply {
                projectId = projectSettings.name
                historyId = testRunStorage.historyId
            }
        }
        set(TEST_MATRIX_FLAKY_TEST_ATTEMPTS_FIELD, projectSettings.maxTestReruns)
        set(TEST_MATRIX_FAIL_FAST_FIELD, projectSettings.failFast)
    }

    private fun createShardingOption(): ShardingOption? {
        val numUniformShards = projectSettings.numUniformShards
        val targetShardDuration = projectSettings.targetedShardDurationSeconds
        return when {
            numUniformShards != 0 && targetShardDuration != 0 -> error(
                "Only one sharding option may be set for numUniformShards or " +
                    "targetedShardDurationMinutes."
            )
            numUniformShards != 0 -> ShardingOption().apply {
                uniformSharding = UniformSharding().apply { numShards = numUniformShards }
            }
            targetShardDuration != 0 -> ShardingOption().apply {
                set(
                    "smartSharding",
                    SmartSharding().apply {
                        targetedShardDuration = "${targetShardDuration}s"
                    },
                )
            }
            else -> null
        }
    }
}

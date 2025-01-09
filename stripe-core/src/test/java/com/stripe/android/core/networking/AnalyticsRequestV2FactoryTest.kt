package com.stripe.android.core.networking

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CLIENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLATFORM_INFO
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLUGIN_TYPE
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_PLATFORM
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_VERSION
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings
import java.net.URLEncoder

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestV2FactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val factory = AnalyticsRequestV2Factory(
        context,
        CLIENT_ID,
        ORIGIN
    )

    private val sdkParamKeys = setOf(
        AnalyticsFields.OS_VERSION,
        PARAM_SDK_PLATFORM,
        PARAM_SDK_VERSION,
        AnalyticsFields.DEVICE_TYPE,
        AnalyticsFields.APP_NAME,
        AnalyticsFields.APP_VERSION,
        AnalyticsFields.DEVICE_ID,
        PARAM_PLUGIN_TYPE,
        PARAM_PLATFORM_INFO
    )

    @Before
    fun setup() {
        // set the android_id to a known value for testing
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "android_id")
    }

    @Test
    fun `verify clientId and origin`() {
        val request = factory.createRequest(
            "EVENT_NAME",
            includeSDKParams = true
        )

        assertThat(request.postParameters.toMap()[PARAM_CLIENT_ID]).isEqualTo(CLIENT_ID)
        assertThat(request.headers[AnalyticsRequestV2.HEADER_ORIGIN]).isEqualTo(ORIGIN)
    }

    @Test
    fun `verify SDKParams are included`() {
        val request = factory.createRequest(
            "EVENT_NAME",
            includeSDKParams = true
        )

        request.postParameters.toMap().let { paramsMap ->
            sdkParamKeys.forEach {
                assertThat(paramsMap).containsKey(it)
            }
        }
    }

    @Test
    fun `SDKParams are not included`() {
        val request = factory.createRequest(
            "EVENT_NAME",
            includeSDKParams = false
        )

        request.postParameters.toMap().let { paramsMap ->
            sdkParamKeys.forEach {
                assertThat(paramsMap).doesNotContainKey(it)
            }
        }
    }

    @Test
    fun `verify additionalParams`() {
        val request = factory.createRequest(
            "EVENT_NAME",
            additionalParams = mapOf(
                "param1" to "value1",
                "param2" to "value2",
                "param3" to mapOf(
                    "nestedParam1" to "nestedValue1",
                    "nestedParam3" to "nestedValue3",
                    "nestedParam2" to "nestedValue2",
                    "nestedParam4" to mapOf(
                        "nestedLv2Param1" to "nestedLv2Value1",
                        "nestedLv2Param3" to "nestedLv2Value3",
                        "nestedLv2Param2" to "nestedLv2Value2",
                        "nestedLv2Param4" to null,
                        "nestedLv2Param5" to ""
                    ),
                    "nestedParam5" to mapOf(
                        "nestedLv2Param6" to mapOf(
                            "nestedLvl3Param1" to "nestedLvl3Param2"
                        )
                    )
                ),
                "param4" to null,
                "param5" to ""
            ),
            includeSDKParams = true
        )

        request.postParameters.toMap().let { paramsMap ->
            assertThat(paramsMap["param1"]).isEqualTo("value1")
            assertThat(paramsMap["param2"]).isEqualTo("value2")
            assertThat(paramsMap["param3"]).isEqualTo(
                URLEncoder.encode(
                    """
                    {
                      "nestedParam1": "nestedValue1",
                      "nestedParam2": "nestedValue2",
                      "nestedParam3": "nestedValue3",
                      "nestedParam4": {
                        "nestedLv2Param1": "nestedLv2Value1",
                        "nestedLv2Param2": "nestedLv2Value2",
                        "nestedLv2Param3": "nestedLv2Value3",
                        "nestedLv2Param5": ""
                      },
                      "nestedParam5": {
                        "nestedLv2Param6": {
                          "nestedLvl3Param1": "nestedLvl3Param2"
                        }
                      }
                    }
                    """.trimIndent(),
                    Charsets.UTF_8.name()
                )
            )
            assertThat(paramsMap.containsKey("param4")).isFalse()
            assertThat(paramsMap["param5"]).isEqualTo("")
        }
    }

    private fun String.toMap(): Map<String, String> {
        val ret = mutableMapOf<String, String>()
        split('&').forEach {
            it.split('=').let { keyValue ->
                ret[keyValue[0]] = keyValue[1]
            }
        }
        return ret
    }

    private companion object {
        const val CLIENT_ID = "test-client-id"
        const val ORIGIN = "test-origin"
    }
}

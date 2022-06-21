package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CLIENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLATFORM_INFO
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLUGIN_TYPE
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_PLATFORM
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_VERSION
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        PARAM_PLUGIN_TYPE,
        PARAM_PLATFORM_INFO
    )

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
        val additionalParam1 = "param1"
        val additionalValue1 = "value1"
        val additionalParam2 = "param2"
        val additionalValue2 = "value2"
        val additionalParam3 = "param3"
        val additionalValue3 = mapOf(
            "nestedParam1" to "nestedValue1",
            "nestedParam3" to "nestedValue3",
            "nestedParam2" to "nestedValue2"
        )

        val request = factory.createRequest(
            "EVENT_NAME",
            additionalParams = mapOf(
                additionalParam1 to additionalValue1,
                additionalParam2 to additionalValue2,
                additionalParam3 to additionalValue3
            ),
            includeSDKParams = true
        )

        request.postParameters.toMap().let { paramsMap ->
            assertThat(paramsMap[additionalParam1]).isEqualTo(additionalValue1)
            assertThat(paramsMap[additionalParam2]).isEqualTo(additionalValue2)
            assertThat(paramsMap[additionalParam3]).isEqualTo(
                URLEncoder.encode(
                    """
                    {
                      "nestedParam1": "nestedValue1",
                      "nestedParam2": "nestedValue2",
                      "nestedParam3": "nestedValue3"
                    }
                    """.trimIndent(),
                    Charsets.UTF_8.name()
                )
            )
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

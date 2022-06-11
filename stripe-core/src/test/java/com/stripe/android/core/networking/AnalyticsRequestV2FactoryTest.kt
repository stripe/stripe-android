package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CLIENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PACKAGE_NAME
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLATFORM_INFO
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_PLUGIN_TYPE
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_PLATFORM
import com.stripe.android.core.networking.AnalyticsRequestV2Factory.Companion.PARAM_SDK_VERSION
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        "$PARAM_PLATFORM_INFO%5B$PARAM_PACKAGE_NAME%5D"
    )

    @Test
    fun `verify clientId and origin`() {
        val requestR = factory.createRequestR(
            "EVENT_NAME",
            includeSDKParams = true
        )

        assertThat(requestR.postParameters.toMap()[PARAM_CLIENT_ID]).isEqualTo(CLIENT_ID)
        assertThat(requestR.headers[AnalyticsRequestV2.HEADER_ORIGIN]).isEqualTo(ORIGIN)
    }

    @Test
    fun `verify SDKParams are included`() {
        val requestR = factory.createRequestR(
            "EVENT_NAME",
            includeSDKParams = true
        )

        requestR.postParameters.toMap().let { paramsMap ->
            sdkParamKeys.forEach {
                assertThat(paramsMap).containsKey(it)
            }
        }
    }

    @Test
    fun `SDKParams are not included`() {
        val requestR = factory.createRequestR(
            "EVENT_NAME",
            includeSDKParams = false
        )

        requestR.postParameters.toMap().let { paramsMap ->
            sdkParamKeys.forEach {
                assertThat(paramsMap).doesNotContainKey(it)
            }
        }
    }

    @Test
    fun `verify additionalParams are included`() {
        val additionalParam1 = "param1"
        val additionalValue1 = "value1"
        val additionalParam2 = "param2"
        val additionalValue2 = "value2"
        val additionalParam3 = "param3"
        val additionalValue3 = "value3"

        val requestR = factory.createRequestR(
            "EVENT_NAME",
            additionalParams = mapOf(
                additionalParam1 to additionalValue1,
                additionalParam2 to additionalValue2,
                additionalParam3 to additionalValue3,
            ),
            includeSDKParams = true
        )

        requestR.postParameters.toMap().let { paramsMap ->
            assertThat(paramsMap[additionalParam1]).isEqualTo(additionalValue1)
            assertThat(paramsMap[additionalParam2]).isEqualTo(additionalValue2)
            assertThat(paramsMap[additionalParam3]).isEqualTo(additionalValue3)
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

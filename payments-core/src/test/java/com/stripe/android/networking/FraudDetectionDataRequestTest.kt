package com.stripe.android.networking

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.FraudDetectionDataFixtures
import com.stripe.android.core.networking.HEADER_CONTENT_TYPE
import com.stripe.android.core.networking.HEADER_X_STRIPE_USER_AGENT
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.version.StripeSdkVersion
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FraudDetectionDataRequestTest {
    private val fraudDetectionDataRequestParamsFactory = FraudDetectionDataRequestParamsFactory(
        ApplicationProvider.getApplicationContext()
    )

    private val request =
        FraudDetectionDataRequest(
            params = fraudDetectionDataRequestParamsFactory.createParams(FRAUD_DETECTION_DATA),
            guid = FRAUD_DETECTION_DATA.guid
        )

    @Test
    fun contentType_shouldBeApplicationJson() {
        assertThat(request.postHeaders?.get(HEADER_CONTENT_TYPE))
            .isEqualTo("application/json; charset=UTF-8")
    }

    @Test
    fun headers_shouldReturnExpectedMap() {
        assertThat(request.headers)
            .isEqualTo(
                mapOf(
                    "Cookie" to "m=${FRAUD_DETECTION_DATA.guid}",
                    "User-Agent" to RequestHeadersFactory.getUserAgent(),
                    "Accept-Charset" to "UTF-8",
                    HEADER_X_STRIPE_USER_AGENT to "{\"lang\":\"kotlin\"," +
                        "\"bindings_version\":\"${StripeSdkVersion.VERSION_NAME}\"," +
                        "\"os_version\":\"${Build.VERSION.SDK_INT}\"," +
                        "\"type\":\"robolectric_robolectric_robolectric\"," +
                        "\"model\":\"robolectric\"}"
                )
            )
    }

    @Test
    fun writeBody_shouldWriteNonEmptyBytes() {
        ByteArrayOutputStream().use {
            request.writePostBody(it)
            assertThat(it.size())
                .isGreaterThan(0)
        }
    }

    private companion object {
        private val FRAUD_DETECTION_DATA = FraudDetectionDataFixtures.create()
    }
}

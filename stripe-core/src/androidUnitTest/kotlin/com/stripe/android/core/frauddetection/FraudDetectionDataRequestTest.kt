package com.stripe.android.core.frauddetection

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.HEADER_CONTENT_TYPE
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import okio.Buffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
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
        assertThat(request.headers["Cookie"]).isEqualTo("m=${FRAUD_DETECTION_DATA.guid}")
    }

    @Test
    fun writeBody_shouldWriteNonEmptyBytes() {
        val buffer = Buffer()
        request.writePostBody(buffer)
        assertThat(buffer.size)
            .isGreaterThan(0L)
    }

    @Test
    fun writeBody_shouldPreserveNestedJsonTypes() {
        val request = FraudDetectionDataRequest(
            params = mapOf(
                "string" to "value",
                "number" to 1,
                "flag" to true,
                "nested" to mapOf(
                    "child" to "nested_value"
                ),
                "list" to listOf(
                    2,
                    "item",
                    false
                )
            ),
            guid = FRAUD_DETECTION_DATA.guid
        )
        val buffer = Buffer()

        request.writePostBody(buffer)

        assertThat(buffer.readUtf8()).isEqualTo(
            """{"string":"value","number":1,"flag":true,"nested":{"child":"nested_value"},"list":[2,"item",false]}"""
        )
    }

    private companion object {
        private val FRAUD_DETECTION_DATA = FraudDetectionDataFixtures.create()
    }
}

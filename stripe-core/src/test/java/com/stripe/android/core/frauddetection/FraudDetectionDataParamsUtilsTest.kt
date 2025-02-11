package com.stripe.android.core.frauddetection

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FraudDetectionDataParamsUtilsTest {

    private val fraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils()

    @Test
    fun addUidParamsToPaymentIntent_withSource_addsParamsAtRightLevel() {
        val updatedParams = fraudDetectionDataParamsUtils.addFraudDetectionData(
            params = mapOf(
                "source_data" to mapOf(
                    "some_field" to 1,
                    "some_other_field" to "foo",
                ),
            ),
            fraudDetectionData = FRAUD_DETECTION_DATA
        )

        assertThat(updatedParams["source_data"])
            .isEqualTo(
                mapOf(
                    "some_field" to 1,
                    "some_other_field" to "foo",
                    "muid" to FRAUD_DETECTION_DATA.muid,
                    "guid" to FRAUD_DETECTION_DATA.guid,
                    "sid" to FRAUD_DETECTION_DATA.sid,
                )
            )
    }

    @Test
    fun addUidParamsToPaymentIntent_withPaymentMethodParams_addsUidAtRightLevel() {
        val updatedParams = fraudDetectionDataParamsUtils.addFraudDetectionData(
            params = mapOf(
                "payment_method_data" to mapOf(
                    "some_field" to 1,
                    "some_other_field" to "foo",
                )
            ),
            fraudDetectionData = FRAUD_DETECTION_DATA
        )
        assertThat(updatedParams["payment_method_data"])
            .isEqualTo(
                mapOf(
                    "some_field" to 1,
                    "some_other_field" to "foo",
                    "muid" to FRAUD_DETECTION_DATA.muid,
                    "guid" to FRAUD_DETECTION_DATA.guid,
                    "sid" to FRAUD_DETECTION_DATA.sid,
                )
            )
    }

    private companion object {
        private val FRAUD_DETECTION_DATA = FraudDetectionDataFixtures.create()
    }
}

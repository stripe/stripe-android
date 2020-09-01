package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardParamsFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.SourceParams
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FingerprintParamsUtilsTest {

    private val fingerprintParamsUtils = FingerprintParamsUtils()

    @Test
    fun addUidParamsToPaymentIntent_withSource_addsParamsAtRightLevel() {
        val updatedParams = fingerprintParamsUtils.addFingerprintData(
            params = mapOf(
                ConfirmPaymentIntentParams.PARAM_SOURCE_DATA to
                    SourceParams.createCardParams(CardParamsFixtures.DEFAULT).toParamMap()
            ),
            fingerprintData = FINGERPRINT_DATA
        )

        assertThat(updatedParams[ConfirmPaymentIntentParams.PARAM_SOURCE_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "card",
                    "owner" to mapOf(
                        "address" to mapOf(
                            "city" to "San Francisco",
                            "country" to "US",
                            "line1" to "123 Market St",
                            "line2" to "#345",
                            "postal_code" to "94107",
                            "state" to "CA"
                        ),
                        "name" to "Jenny Rosen"
                    ),
                    "card" to mapOf(
                        "number" to CardNumberFixtures.VISA_NO_SPACES,
                        "exp_month" to 12,
                        "exp_year" to 2025,
                        "cvc" to "123"
                    ),
                    "metadata" to mapOf("fruit" to "orange"),
                    "muid" to FINGERPRINT_DATA.muid,
                    "guid" to FINGERPRINT_DATA.guid,
                    "sid" to FINGERPRINT_DATA.sid
                )
            )
    }

    @Test
    fun addUidParamsToPaymentIntent_withPaymentMethodParams_addsUidAtRightLevel() {
        val updatedParams = fingerprintParamsUtils.addFingerprintData(
            params = mapOf(
                PARAM_PAYMENT_METHOD_DATA to
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap()
            ),
            fingerprintData = FINGERPRINT_DATA
        )
        assertThat(updatedParams[PARAM_PAYMENT_METHOD_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "card",
                    "billing_details" to mapOf(
                        "address" to mapOf(
                            "city" to "San Francisco",
                            "country" to "US",
                            "line1" to "1234 Main St",
                            "state" to "CA",
                            "postal_code" to "94111"
                        ),
                        "email" to "jenny.rosen@example.com",
                        "name" to "Jenny Rosen",
                        "phone" to "1-800-555-1234"
                    ),
                    "card" to mapOf(
                        "number" to CardNumberFixtures.VISA_NO_SPACES,
                        "exp_month" to 1,
                        "exp_year" to 2024,
                        "cvc" to "111"
                    ),
                    "muid" to FINGERPRINT_DATA.muid,
                    "guid" to FINGERPRINT_DATA.guid,
                    "sid" to FINGERPRINT_DATA.sid
                )
            )
    }

    private companion object {
        private val FINGERPRINT_DATA = FingerprintDataFixtures.create()
    }
}

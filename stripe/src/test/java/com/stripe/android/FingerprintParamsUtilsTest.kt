package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.SourceParams
import java.util.UUID
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintParamsUtilsTest {

    private val fingerprintParamsUtils = FingerprintParamsUtils(
        ApiFingerprintParamsFactory(
            store = FakeClientFingerprintDataStore(MUID)
        )
    )

    @Test
    fun addUidParamsToPaymentIntent_withSource_addsParamsAtRightLevel() {
        val updatedParams = fingerprintParamsUtils.addFingerprintData(
            params = mapOf(
                ConfirmPaymentIntentParams.PARAM_SOURCE_DATA to
                    SourceParams.createCardParams(CardFixtures.CARD).toParamMap()
            ),
            fingerprintGuid = GUID.toString()
        )

        assertThat(updatedParams[ConfirmPaymentIntentParams.PARAM_SOURCE_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "card",
                    "owner" to mapOf(
                        "address" to mapOf(
                            "city" to "San Francisco",
                            "country" to "US",
                            "line1" to "123 Main Street",
                            "line2" to "906",
                            "postal_code" to "94107",
                            "state" to "CA"
                        ),
                        "name" to "J Q Public"
                    ),
                    "card" to mapOf(
                        "number" to CardNumberFixtures.VISA_NO_SPACES,
                        "exp_month" to 8,
                        "exp_year" to 2019,
                        "cvc" to "123"
                    ),
                    "muid" to MUID.toString(),
                    "guid" to GUID.toString()
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
            fingerprintGuid = GUID.toString()
        )
        assertThat(updatedParams[PARAM_PAYMENT_METHOD_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "card",
                    "billing_details" to mapOf(
                        "address" to mapOf(
                            "city" to "Los Angeles",
                            "country" to "US",
                            "line1" to "123 Main St",
                            "state" to "CA",
                            "postal_code" to "90012"
                        ),
                        "email" to "me@example.com",
                        "name" to "Home",
                        "phone" to "1-800-555-1234"
                    ),
                    "card" to mapOf(
                        "number" to CardNumberFixtures.VISA_NO_SPACES,
                        "exp_month" to 1,
                        "exp_year" to 2024,
                        "cvc" to "111"
                    ),
                    "muid" to MUID.toString(),
                    "guid" to GUID.toString()
                )
            )
    }

    private companion object {
        private val GUID = UUID.randomUUID()
        private val MUID = UUID.randomUUID()
    }
}

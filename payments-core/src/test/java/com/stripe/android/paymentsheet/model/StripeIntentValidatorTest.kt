package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
class StripeIntentValidatorTest {
    private val validator = StripeIntentValidator()

    @Test
    fun `requireValid() should return original PaymentIntent when valid`() {
        assertThat(
            validator.requireValid(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        ).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    }

    @Test
    fun `requireValid() requires confirmationMethod = Automatic`() {
        assertFails {
            validator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            )
        }
    }

    @Test
    fun `requireValid() processing is not valid`() {
        assertFails {
            validator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    status = StripeIntent.Status.Processing
                )
            )
        }
    }

    @Test
    fun `PaymentIntent requireValid() allows status = RequiresPaymentMethod`() {
        validator.requireValid(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        )
    }

    @Test
    fun `PaymentIntent requireValid() allows status = RequiresAction`() {
        validator.requireValid(
            PaymentIntentJsonParser().parse(PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON)!!,
        )
    }

    @Test
    fun `SetupIntent requireValid() allows status = RequiresPaymentMethod`() {
        validator.requireValid(
            SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
    }

    @Test
    fun `SetupIntent requireValid() allows status = RequiresAction`() {
        validator.requireValid(
            SetupIntentJsonParser().parse(
                SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT_JSON
            )!!
        )
    }
}

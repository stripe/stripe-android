package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
class StripeIntentValidatorTest {

    @Test
    fun `requireValid() should return original PaymentIntent when valid`() {
        assertThat(
            StripeIntentValidator.requireValid(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        ).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    }

    @Test
    fun `requireValid() requires confirmationMethod = Automatic`() {
        assertFails {
            StripeIntentValidator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            )
        }
    }

    @Test
    fun `requireValid() Succeeded is not valid`() {
        assertFails {
            StripeIntentValidator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    status = StripeIntent.Status.Succeeded
                )
            )
        }
    }

    @Test
    fun `PaymentIntent requireValid() allows status = RequiresPaymentMethod`() {
        StripeIntentValidator.requireValid(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        )
    }

    @Test
    fun `PaymentIntent requireValid() allows status = RequiresAction`() {
        StripeIntentValidator.requireValid(
            PaymentIntentJsonParser().parse(PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON)!!
        )
    }

    @Test
    fun `SetupIntent requireValid() allows status = RequiresPaymentMethod`() {
        StripeIntentValidator.requireValid(
            SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
    }

    @Test
    fun `SetupIntent requireValid() allows status = RequiresAction`() {
        StripeIntentValidator.requireValid(
            SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
        )
    }

    @Test
    fun `PaymentIntent requireValid() allows status = RequiresConfirmation`() {
        StripeIntentValidator.requireValid(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                status = StripeIntent.Status.RequiresConfirmation
            )
        )
    }

    @Test
    fun `PaymentIntent requireValid() allows status = Processing`() {
        StripeIntentValidator.requireValid(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                status = StripeIntent.Status.Processing
            )
        )
    }

    @Test
    fun `Considers PaymentIntent without amount invalid`() {
        assertFails {
            StripeIntentValidator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    amount = null,
                )
            )
        }
    }

    @Test
    fun `Considers PaymentIntent without currency invalid`() {
        assertFails {
            StripeIntentValidator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    currency = null,
                )
            )
        }
    }
}

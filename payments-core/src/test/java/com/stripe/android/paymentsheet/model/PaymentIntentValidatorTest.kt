package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
class PaymentIntentValidatorTest {
    private val validator = PaymentIntentValidator()

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
    fun `requireValid() requires status = RequiresPaymentMethod`() {
        assertFails {
            validator.requireValid(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    status = StripeIntent.Status.Processing
                )
            )
        }
    }
}

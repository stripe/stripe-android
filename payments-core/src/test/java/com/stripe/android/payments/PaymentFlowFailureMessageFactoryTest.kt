package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentFlowFailureMessageFactoryTest {

    private val factory = PaymentFlowFailureMessageFactory(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `create() with PaymentIntent with lastPaymentError`() {
        assertThat(
            factory.create(
                PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
                StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with SetupIntent with lastPaymentError`() {
        assertThat(
            factory.create(
                SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.copy(
                    status = StripeIntent.Status.RequiresPaymentMethod
                ),
                StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with timed out outcome`() {
        assertThat(
            factory.create(
                SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                StripeIntentResult.Outcome.TIMEDOUT
            )
        ).isEqualTo(
            "Timed out authenticating your payment method -- try again"
        )
    }
}

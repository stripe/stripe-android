package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentFlowFailureMessageFactoryTest {

    private val factory = PaymentFlowFailureMessageFactory(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `create() with PaymentIntent with requiresAction on a card`() {
        assertThat(
            factory.create(
                intent = PaymentIntentJsonParser().parse(PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON)!!,
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with PaymentIntent with requiresAction on oxxo`() {
        assertThat(
            factory.create(
                intent = PaymentIntentJsonParser().parse(PaymentIntentFixtures.OXXO_REQUIRES_ACTION_JSON)!!,
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isNull()
    }

    @Test
    fun `create() with PaymentIntent with lastPaymentError`() {
        assertThat(
            factory.create(
                intent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with SetupIntent with lastPaymentError`() {
        assertThat(
            factory.create(
                intent = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.copy(
                    status = StripeIntent.Status.RequiresPaymentMethod
                ),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with timed out outcome`() {
        assertThat(
            factory.create(
                intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                requestId = null,
                outcome = StripeIntentResult.Outcome.TIMEDOUT
            )
        ).isEqualTo(
            "Timed out authenticating your payment method -- try again"
        )
    }

    @Test
    fun `create() uses requestId with default messaging in live mode`() {
        assertThat(
            factory.create(
                intent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.copy(
                    lastPaymentError = PaymentIntent.Error(
                        code = "generic_deline",
                        declineCode = null,
                        charge = null,
                        docUrl = null,
                        paymentMethod = null,
                        param = null,
                        message = "Server error",
                        type = PaymentIntent.Error.Type.ApiError,
                    ),
                    isLiveMode = true
                ),
                requestId = "req_abc123",
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "Something went wrong. req_abc123"
        )
    }
}

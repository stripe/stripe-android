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
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.Locale
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
                PaymentIntentJsonParser().parse(PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON)!!,
                StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "We are unable to authenticate your payment method. Please choose a different payment method and try again."
        )
    }

    @Test
    fun `create() with PaymentIntent with requiresAction on oxxo`() {
        assertThat(
            factory.create(
                PaymentIntentJsonParser().parse(PaymentIntentFixtures.OXXO_REQUIRES_ACTION_JSON)!!,
                StripeIntentResult.Outcome.FAILED
            )
        ).isNull()
    }

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

    @Test
    fun `Uses error message from backend if using suitable locale`() = withLocale(Locale.US) {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            lastPaymentError = PaymentIntent.Error(
                code = "card_declined",
                message = "this is a message that should be shown",
                paymentMethod = mock(),
                type = PaymentIntent.Error.Type.CardError,
                charge = null,
                declineCode = null,
                docUrl = null,
                param = null,
            )
        )

        val result = factory.create(intent, StripeIntentResult.Outcome.FAILED)
        assertThat(result).isEqualTo("this is a message that should be shown")
    }

    @Test
    fun `Uses local error message if using locale that we can't properly handle`() = withLocale(
        locale = Locale("es", "ar"),
    ) {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            lastPaymentError = PaymentIntent.Error(
                code = "card_declined",
                message = "this is a message that shouldn't be shown",
                paymentMethod = mock(),
                type = PaymentIntent.Error.Type.CardError,
                charge = null,
                declineCode = null,
                docUrl = null,
                param = null,
            )
        )

        val result = factory.create(intent, StripeIntentResult.Outcome.FAILED)
        assertThat(result).isEqualTo("Your card was declined")
    }
}

private fun withLocale(locale: Locale, block: () -> Unit) {
    val original = Locale.getDefault()
    Locale.setDefault(locale)

    try {
        block()
    } finally {
        Locale.setDefault(original)
    }
}

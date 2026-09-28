package com.stripe.android.payments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.testing.LocaleTestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentFlowFailureMessageFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val localeTestRule = LocaleTestRule()

    private val factory = PaymentFlowFailureMessageFactory(context)

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
    fun `create() with declined 3DS2 PaymentIntent returns localized decline message (en)`() {
        assertThat(
            factory.create(
                intent = declined3ds2PaymentIntent(),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "Your card has insufficient funds."
        )
    }

    @Test
    fun `create() with declined 3DS2 PaymentIntent returns localized decline message (fr)`() {
        localeTestRule.setTemporarily(Locale.FRANCE)

        val localizedFactory = PaymentFlowFailureMessageFactory(
            localeTestRule.contextForLocale(context)
        )

        assertThat(
            localizedFactory.create(
                intent = declined3ds2PaymentIntent(),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "Votre carte ne dispose pas de fonds suffisants."
        )
    }

    @Test
    fun `create() with succeeded 3DS2 PaymentIntent returns null`() {
        assertThat(
            factory.create(
                intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    status = StripeIntent.Status.Succeeded
                ),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isNull()
    }

    @Test
    fun `create() with 3DS2 PaymentIntent still mid-authentication returns null`() {
        // PI_VISA_3DS2 is a genuine 3DS2 intent still in RequiresAction with no error
        // (e.g. the customer canceled). Without the 3DS2 guard this would surface the
        // generic authentication-failure message instead of null.
        assertThat(
            factory.create(
                intent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                    status = StripeIntent.Status.RequiresAction
                ),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isNull()
    }

    @Test
    fun `create() with declined 3DS2 SetupIntent returns localized decline message`() {
        assertThat(
            factory.create(
                intent = declined3ds2SetupIntent(),
                requestId = null,
                outcome = StripeIntentResult.Outcome.FAILED
            )
        ).isEqualTo(
            "Your card has insufficient funds."
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
            "Something went wrong. Request ID: req_abc123"
        )
    }

    private fun declined3ds2PaymentIntent(): PaymentIntent {
        // PI_VISA_3DS2 is a genuine 3DS2 intent (card payment method + Use3DS2 next
        // action). A RequiresPaymentMethod status with a card_declined error models a
        // card that was declined during the 3DS2 flow, which must still surface the
        // localized decline message rather than being suppressed as "mid-authentication".
        return PaymentIntentFixtures.PI_VISA_3DS2.copy(
            status = StripeIntent.Status.RequiresPaymentMethod,
            lastPaymentError = PaymentIntent.Error(
                code = "card_declined",
                declineCode = "insufficient_funds",
                type = PaymentIntent.Error.Type.CardError,
                charge = null,
                docUrl = null,
                message = "Your card has insufficient funds.",
                param = null,
                paymentMethod = null,
            ),
            isLiveMode = false,
        )
    }

    private fun declined3ds2SetupIntent(): SetupIntent {
        // SI_3DS2_PROCESSING is a genuine 3DS2 SetupIntent (card payment method +
        // Use3DS2 next action). A RequiresPaymentMethod status with a card_declined
        // error models a card declined during the 3DS2 flow.
        return SetupIntentFixtures.SI_3DS2_PROCESSING.copy(
            status = StripeIntent.Status.RequiresPaymentMethod,
            lastSetupError = SetupIntent.Error(
                code = "card_declined",
                declineCode = "insufficient_funds",
                docUrl = null,
                message = "Your card has insufficient funds.",
                param = null,
                paymentMethod = null,
                type = SetupIntent.Error.Type.CardError,
            ),
            isLiveMode = false,
        )
    }
}

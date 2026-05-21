package com.stripe.android.payments.paymentlauncher

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.RateLimitException
import com.stripe.android.exception.CardException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeIntentKtxTest {

    @Test
    fun `toFailureThrowable returns CardException for payment intent card errors and prefers failure message`() {
        val error = requireNotNull(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.lastPaymentError).copy(
            type = PaymentIntent.Error.Type.CardError,
            message = "intent error message",
            code = "card_declined",
            param = "payment_method",
            declineCode = "do_not_honor",
            charge = "ch_123",
            docUrl = "https://stripe.com/docs/error-codes/card-declined",
        )

        val throwable = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.copy(lastPaymentError = error),
            failureMessage = "launcher failure message",
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(CardException::class.java)
        assertThat(throwable.message).isEqualTo("launcher failure message")
        assertThat(throwable.stripeError?.type).isEqualTo(PaymentIntent.Error.Type.CardError.code)
        assertThat(throwable.stripeError?.code).isEqualTo("card_declined")
        assertThat(throwable.stripeError?.param).isEqualTo("payment_method")
        assertThat(throwable.stripeError?.declineCode).isEqualTo("do_not_honor")
        assertThat(throwable.stripeError?.charge).isEqualTo("ch_123")
        assertThat(throwable.stripeError?.docUrl)
            .isEqualTo("https://stripe.com/docs/error-codes/card-declined")
    }

    @Test
    fun `toFailureThrowable returns AuthenticationException for payment intent authentication errors`() {
        val error = requireNotNull(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.lastPaymentError).copy(
            type = PaymentIntent.Error.Type.AuthenticationError,
            message = "auth error message",
        )

        val throwable = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.copy(lastPaymentError = error),
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(AuthenticationException::class.java)
        assertThat(throwable.message).isEqualTo("auth error message")
    }

    @Test
    fun `toFailureThrowable returns RateLimitException for payment intent rate limit errors`() {
        val error = requireNotNull(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.lastPaymentError).copy(
            type = PaymentIntent.Error.Type.RateLimitError,
            message = "rate limit error message",
        )

        val throwable = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.copy(lastPaymentError = error),
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(RateLimitException::class.java)
        assertThat(throwable.message).isEqualTo("rate limit error message")
    }

    @Test
    fun `toFailureThrowable returns InvalidRequestException for setup intent invalid request errors`() {
        val error = requireNotNull(SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.lastSetupError).copy(
            type = SetupIntent.Error.Type.InvalidRequestError,
            message = "setup error message",
            code = "resource_missing",
            param = "client_secret",
            declineCode = "generic_decline",
            docUrl = "https://stripe.com/docs/error-codes/resource-missing",
        )

        val throwable = SetupIntentResult(
            intent = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.copy(lastSetupError = error),
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(InvalidRequestException::class.java)
        assertThat(throwable.message).isEqualTo("setup error message")
        assertThat(throwable.stripeError?.type)
            .isEqualTo(SetupIntent.Error.Type.InvalidRequestError.code)
        assertThat(throwable.stripeError?.code).isEqualTo("resource_missing")
        assertThat(throwable.stripeError?.param).isEqualTo("client_secret")
        assertThat(throwable.stripeError?.declineCode).isEqualTo("generic_decline")
        assertThat(throwable.stripeError?.docUrl)
            .isEqualTo("https://stripe.com/docs/error-codes/resource-missing")
        assertThat(throwable.stripeError?.charge).isNull()
    }

    @Test
    fun `toFailureThrowable returns APIException for unmapped setup intent errors`() {
        val error = requireNotNull(SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.lastSetupError).copy(
            type = SetupIntent.Error.Type.ApiError,
            message = "api error message",
        )

        val throwable = SetupIntentResult(
            intent = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.copy(lastSetupError = error),
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(APIException::class.java)
        assertThat(throwable.message).isEqualTo("api error message")
    }

    @Test
    fun `toFailureThrowable returns LocalStripeException when intent has no last error`() {
        val throwable = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            failureMessage = "launcher failure message",
        ).toFailureThrowable()

        assertThat(throwable).isInstanceOf(LocalStripeException::class.java)
        throwable as LocalStripeException
        assertThat(throwable.displayMessage).isEqualTo("launcher failure message")
        assertThat(throwable.analyticsValue).isEqualTo("failedIntentOutcomeError")
    }
}

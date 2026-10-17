package com.stripe.android.common.exception

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.exception.CardException
import com.stripe.android.paymentsheet.R
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class StripeErrorMessageTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun testApiConnectionException() {
        assertThatStripeErrorMessage(APIConnectionException.create(IOException("Foobar")))
            .isEqualTo("An error occurred. Check your connection and try again.")
    }

    @Test
    fun testLocalStripeException() {
        assertThatStripeErrorMessage(LocalStripeException("Hi mom", null))
            .isEqualTo("Hi mom")
    }

    @Test
    fun testCardErrorTypeShowsRawMessage() {
        assertThatStripeErrorMessage(
            CardException(StripeError(type = "card_error", message = "Your card was declined."))
        ).isEqualTo("Your card was declined.")
    }

    @Test
    fun testNonCardErrorTypeShowsGenericMessage() {
        assertThatStripeErrorMessage(
            InvalidRequestException(
                stripeError = StripeError(type = "invalid_request_error", message = "Developer message")
            )
        ).isEqualTo("Something went wrong")
    }

    @Test
    fun testStripeExceptionWithoutStripeErrorMessage() {
        assertThatStripeErrorMessage(InvalidRequestException())
            .isEqualTo("Something went wrong")
    }

    @Test
    fun testIllegalStateException() {
        assertThatStripeErrorMessage(IllegalStateException("Hi mom"))
            .isEqualTo("Something went wrong")
    }

    @Test
    fun testApiConnectionExceptionWithResolvableString() {
        assertThatResolvableStripeErrorMessage(APIConnectionException.create(IOException("Foobar")))
            .isEqualTo(R.string.stripe_network_error_message.resolvableString)
    }

    @Test
    fun testLocalStripeExceptionWithResolvableString() {
        assertThatResolvableStripeErrorMessage(LocalStripeException("Hi mom", null))
            .isEqualTo("Hi mom".resolvableString)
    }

    @Test
    fun testCardErrorTypeShowsRawMessageWithResolvableString() {
        assertThatResolvableStripeErrorMessage(
            CardException(StripeError(type = "card_error", message = "Your card was declined."))
        ).isEqualTo("Your card was declined.".resolvableString)
    }

    @Test
    fun testNonCardErrorTypeShowsGenericMessageWithResolvableString() {
        assertThatResolvableStripeErrorMessage(
            InvalidRequestException(
                stripeError = StripeError(type = "invalid_request_error", message = "Developer message")
            )
        ).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
    }

    @Test
    fun testStripeExceptionWithoutStripeErrorMessageWithResolvableString() {
        assertThatResolvableStripeErrorMessage(InvalidRequestException())
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
    }

    @Test
    fun testIllegalStateExceptionWithResolvableString() {
        assertThatResolvableStripeErrorMessage(IllegalStateException("Hi mom"))
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
    }

    @Test
    fun terminalException_usesErrorMessage() {
        val expectedErrorMessage = "Transaction timed out."
        assertThatResolvableStripeErrorMessage(
            TerminalException(
                errorCode = TerminalErrorCode.CARD_READ_TIMED_OUT,
                errorMessage = expectedErrorMessage
            )
        ).isEqualTo(expectedErrorMessage.resolvableString)
    }

    private fun assertThatStripeErrorMessage(throwable: Throwable): StringSubject {
        return assertThat(throwable.stripeErrorMessage(application))
    }

    private fun assertThatResolvableStripeErrorMessage(throwable: Throwable): Subject {
        return assertThat(throwable.stripeErrorMessage())
    }
}

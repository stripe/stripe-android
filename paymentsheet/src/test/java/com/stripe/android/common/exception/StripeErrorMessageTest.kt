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
    fun testStripeExceptionWithStripeErrorMessage() {
        assertThatStripeErrorMessage(CardException(StripeError(message = "From the server")))
            .isEqualTo("From the server")
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
            .isEqualTo(resolvableString(R.string.stripe_network_error_message))
    }

    @Test
    fun testLocalStripeExceptionWithResolvableString() {
        assertThatResolvableStripeErrorMessage(LocalStripeException("Hi mom", null))
            .isEqualTo(resolvableString("Hi mom"))
    }

    @Test
    fun testStripeExceptionWithStripeErrorMessageWithResolvableString() {
        assertThatResolvableStripeErrorMessage(CardException(StripeError(message = "From the server")))
            .isEqualTo(resolvableString("From the server"))
    }

    @Test
    fun testStripeExceptionWithoutStripeErrorMessageWithResolvableString() {
        assertThatResolvableStripeErrorMessage(InvalidRequestException())
            .isEqualTo(resolvableString(R.string.stripe_something_went_wrong))
    }

    @Test
    fun testIllegalStateExceptionWithResolvableString() {
        assertThatResolvableStripeErrorMessage(IllegalStateException("Hi mom"))
            .isEqualTo(resolvableString(R.string.stripe_something_went_wrong))
    }

    private fun assertThatStripeErrorMessage(throwable: Throwable): StringSubject {
        return assertThat(throwable.stripeErrorMessage(application))
    }

    private fun assertThatResolvableStripeErrorMessage(throwable: Throwable): Subject {
        return assertThat(throwable.stripeErrorMessage())
    }
}

package com.stripe.android.common.exception

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.exception.CardException
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
        assertThatStripeErrorMessage(LocalStripeException("Hi mom"))
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

    private fun assertThatStripeErrorMessage(throwable: Throwable): StringSubject {
        return assertThat(throwable.stripeErrorMessage(application))
    }
}

package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.paymentsheet.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class CheckoutErrorMapperTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val mapper = CheckoutErrorMapper(application)

    @Test
    fun `uses local display message`() {
        val result = mapper.displayMessage(LocalStripeException("Local display message", null))

        assertThat(result).isEqualTo("Local display message")
    }

    @Test
    fun `uses typed server message for every stripe error type`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(type = "api_error", message = "Server display message"),
            ),
        )

        assertThat(result).isEqualTo("Server display message")
    }

    @Test
    fun `uses server message when error type is missing`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(message = "Untyped server display message"),
            ),
        )

        assertThat(result).isEqualTo("Untyped server display message")
    }

    @Test
    fun `uses generic message when server response is malformed`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(message = StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE),
                message = StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE,
            ),
        )

        assertThat(result).isEqualTo(application.getString(R.string.stripe_something_went_wrong))
    }

    @Test
    fun `uses generic message when typed server error has no display message`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(type = "api_error"),
                message = "Request failed with status code 400 and non-JSON error body.",
            ),
        )

        assertThat(result).isEqualTo(application.getString(R.string.stripe_something_went_wrong))
    }

    @Test
    fun `uses localized network fallback`() {
        val error = APIConnectionException.create(IOException("connection diagnostic"))

        assertThat(mapper.displayMessage(error))
            .isEqualTo(application.getString(R.string.stripe_network_error_message))
    }
}

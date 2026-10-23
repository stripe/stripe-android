package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import org.junit.Test
import java.io.IOException

internal class CheckoutErrorMapperTest {
    private val mapper = CheckoutErrorMapper()

    @Test
    fun `uses local display message`() {
        val result = mapper.displayMessage(LocalStripeException("Local display message", null))

        assertThat(result).isEqualTo("Local display message")
    }

    @Test
    fun `uses server message for non-card errors`() {
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
    fun `returns null when server response is malformed`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(message = StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE),
                message = StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE,
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when typed server error has no display message`() {
        val result = mapper.displayMessage(
            APIException(
                stripeError = StripeError(type = "api_error"),
                message = "Request failed with status code 400 and non-JSON error body.",
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for network errors`() {
        val error = APIConnectionException.create(IOException("connection diagnostic"))

        assertThat(mapper.displayMessage(error)).isNull()
    }
}

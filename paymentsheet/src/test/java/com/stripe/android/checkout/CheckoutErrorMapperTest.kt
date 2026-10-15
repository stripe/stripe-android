package com.stripe.android.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.paymentsheet.R
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutErrorMapperTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mapper = CheckoutErrorMapper(context)

    @Test
    fun `server message is passed through for non-card errors`() {
        val error = APIException(
            stripeError = StripeError(type = "invalid_request_error", message = "Invalid promotion code"),
        )

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo("Invalid promotion code")
        assertThat(error.message).isEqualTo("Invalid promotion code")
    }

    @Test
    fun `server message without a type is passed through`() {
        val stripeError = StripeErrorJsonParser().parse(
            JSONObject("""{"error":{"message":"Invalid promotion code"}}"""),
        )
        val error = APIException(stripeError = stripeError)

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo("Invalid promotion code")
        assertThat(error.message).isEqualTo("Invalid promotion code")
    }

    @Test
    fun `missing server message uses localized generic text and preserves diagnostics`() {
        val error = APIException(
            stripeError = StripeError(type = "api_error"),
            message = "Request failed with status code 500 and non-JSON error body.",
        )

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo(context.getString(R.string.stripe_something_went_wrong))
        assertThat(error.message).isEqualTo("Request failed with status code 500 and non-JSON error body.")
    }

    @Test
    fun `malformed error parser diagnostics are not displayed`() {
        val error = APIException(stripeError = StripeErrorJsonParser().parse(JSONObject("{}")))

        mapper.populateDisplayMessage(error)

        assertThat(error.message).isEqualTo("An improperly formatted error response was found.")
        assertThat(error.displayMessage).isEqualTo(context.getString(R.string.stripe_something_went_wrong))
    }

    @Test
    fun `network errors use the existing localized message`() {
        val error = APIConnectionException(message = "Failed to execute a request")

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo(context.getString(R.string.stripe_network_error_message))
        assertThat(error.message).isEqualTo("Failed to execute a request")
    }

    @Test
    fun `local display messages are preserved`() {
        val error = LocalStripeException(displayMessage = "Try another payment method", analyticsValue = "local")

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo("Try another payment method")
    }

    @Test
    fun `local errors without display text receive the generic fallback`() {
        val error = LocalStripeException(displayMessage = null, analyticsValue = "local")
        assertThat(error.displayMessage).isNull()

        mapper.populateDisplayMessage(error)

        assertThat(error.displayMessage).isEqualTo(context.getString(R.string.stripe_something_went_wrong))
        assertThat(error.message).isNull()
    }

    @Test
    fun `ordinary Stripe errors have no checkout display message`() {
        val error = APIException(
            stripeError = StripeError(type = "invalid_request_error", message = "Diagnostic server text"),
        )

        assertThat(error.displayMessage).isNull()
    }
}

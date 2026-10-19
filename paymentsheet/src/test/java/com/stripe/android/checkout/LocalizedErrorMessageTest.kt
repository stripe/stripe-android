package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(TestParameterInjector::class)
internal class LocalizedErrorMessageTest {
    @Test
    fun `returns server message regardless of error type`(
        @TestParameter(values = ["card_error", "invalid_request_error", "api_error"]) type: String,
    ) {
        val error = APIException(
            stripeError = StripeError(type = type, message = "Ce code promotionnel est invalide."),
            message = "Diagnostic message",
        )

        assertThat(error.localizedErrorMessage()).isEqualTo("Ce code promotionnel est invalide.")
    }

    @Test
    fun `returns server message without an error type`() {
        val error = APIException(stripeError = StripeError(message = "No such promotion code: ABCD"))

        assertThat(error.localizedErrorMessage()).isEqualTo("No such promotion code: ABCD")
    }

    @Test
    fun `returns null when server message is missing`() {
        val error = APIException(
            stripeError = StripeError(type = "invalid_request_error"),
            message = "Request failed with status code 400 and non-JSON error body.",
        )

        assertThat(error.localizedErrorMessage()).isNull()
    }

    @Test
    fun `returns null when Stripe error is missing`() {
        val error = APIException(message = "Failed to parse response JSON")

        assertThat(error.localizedErrorMessage()).isNull()
    }

    @Test
    fun `returns null for SDK errors without a Stripe error`() {
        val error = IllegalStateException("Cannot mutate checkout session before it is configured.")

        assertThat(error.localizedErrorMessage()).isNull()
    }
}

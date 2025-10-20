package com.stripe.android.link.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ErrorsTest {

    @Test
    fun `isLinkAuthorizationError returns true for consumer_session_credentials_invalid`() {
        val stripeError = StripeError(
            code = "consumer_session_credentials_invalid",
            message = "Consumer session credentials are invalid"
        )
        val apiException = APIException(stripeError)

        assertThat(apiException.isLinkAuthorizationError()).isTrue()
    }

    @Test
    fun `isLinkAuthorizationError returns false for other API error codes`() {
        val stripeError = StripeError(
            code = "card_declined",
            message = "Your card was declined"
        )
        val apiException = APIException(stripeError)

        assertThat(apiException.isLinkAuthorizationError()).isFalse()
    }

    @Test
    fun `isLinkAuthorizationError returns false for null error code`() {
        val stripeError = StripeError(
            code = null,
            message = "Unknown error"
        )
        val apiException = APIException(stripeError)

        assertThat(apiException.isLinkAuthorizationError()).isFalse()
    }

    @Test
    fun `isLinkAuthorizationError returns false for APIException with null stripeError`() {
        val apiException = APIException()

        assertThat(apiException.isLinkAuthorizationError()).isFalse()
    }

    @Test
    fun `isLinkAuthorizationError returns true for non-API exception`() {
        val stripeError = StripeError(
            code = "consumer_session_credentials_invalid",
            message = "Consumer session credentials are invalid"
        )
        val invalidRequestException = InvalidRequestException(stripeError, requestId = null)

        assertThat(invalidRequestException.isLinkAuthorizationError()).isTrue()
    }

    @Test
    fun `isLinkAuthorizationError returns false for RuntimeException`() {
        val runtimeException = RuntimeException("Network error")

        assertThat(runtimeException.isLinkAuthorizationError()).isFalse()
    }
}

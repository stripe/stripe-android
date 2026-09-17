package com.stripe.android.crypto.onramp.exception

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.crypto.onramp.R
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent.ErrorOccurred.Operation
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnrampErrorMapperTest {
    @Test
    fun `non-Stripe exception maps to first-class Onramp error`() {
        val underlyingError = IllegalStateException("Unexpected SDK failure")

        val result = underlyingError.toCryptoOnrampError(
            context = createContext(),
            operation = Operation.FulfillAdditionalKycRequirement,
            publishableKey = "pk_test_123",
        )

        assertThat(result).isInstanceOf(StripeCryptoOnrampError::class.java)
        assertThat(result).isInstanceOf(UnexpectedException::class.java)
        val error = result as UnexpectedException
        assertThat(error.code).isEqualTo("unexpected_error")
        assertThat(error.userMessage).isEqualTo("Something went wrong. Please try again later.")
        assertThat(error.developerMessage).contains("Unexpected SDK failure")
        assertThat(error.developerMessage).contains("operation: fulfill_additional_kyc_requirement")
        assertThat(error.underlyingError).isSameInstanceAs(underlyingError)
        assertThat(error.docUrl).isNull()
    }

    @Test
    fun `Stripe exception without structured API error maps to first-class Onramp error`() {
        val underlyingError = APIException(message = "Unstructured API failure")

        val result = underlyingError.toCryptoOnrampError(
            context = createContext(),
            operation = Operation.VerifyIdentity,
            publishableKey = "pk_live_123",
        )

        assertThat(result).isInstanceOf(UnexpectedException::class.java)
        val error = result as UnexpectedException
        assertThat(error.developerMessage).contains("Unstructured API failure")
        assertThat(error.developerMessage).contains("operation: verify_identity")
        assertThat(error.developerMessage).contains("mode: live")
        assertThat(error.underlyingError).isSameInstanceAs(underlyingError)
    }

    @Test
    fun `first-class Onramp error is not wrapped again`() {
        val underlyingError = IllegalStateException("Unexpected SDK failure")
        val firstMapping = underlyingError.toCryptoOnrampError(
            context = createContext(),
            operation = Operation.Configure,
            publishableKey = null,
        )

        val secondMapping = firstMapping.toCryptoOnrampError(
            context = createContext(),
            operation = Operation.Authorize,
            publishableKey = null,
        )

        assertThat(secondMapping).isSameInstanceAs(firstMapping)
    }

    private fun createContext(): Context {
        val context = mock<Context>()
        whenever(context.packageName).thenReturn("com.stripe.android.crypto.onramp.test")
        whenever(context.getString(R.string.stripe_onramp_default_api_error_user_message))
            .thenReturn("Something went wrong. Please try again later.")
        return context
    }
}

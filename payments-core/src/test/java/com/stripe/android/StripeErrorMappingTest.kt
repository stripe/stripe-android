package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.withLocalizedMessage
import com.stripe.android.testing.LocaleTestRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StripeErrorMappingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val localeTestRule = LocaleTestRule()

    @Test
    fun `Uses backend message for card error `() {
        runStripeErrorMappingTest(
            message = "The backend message",
            code = null,
            declineCode = null,
            context = context
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("The backend message")
        }
    }

    @Test
    fun `Uses localized client error for error code`() {
        runStripeErrorMappingTest(
            message = null,
            type = "card_error",
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

    @Test
    fun `Defer to decline code first`() {
        runStripeErrorMappingTest(
            message = null,
            declineCode = "insufficient_funds",
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your card has insufficient funds.")
        }
    }

    @Test
    fun `Uses localized client error for error code, Locale is not US`() {
        localeTestRule.setTemporarily(Locale.FRANCE)

        runStripeErrorMappingTest(
            message = null,
            type = "card_error",
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Votre moyen de paiement a été refusé.")
        }
    }

    @Test
    fun `Uses localized client error for valid decline code`() {
        runStripeErrorMappingTest(
            code = null,
            declineCode = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

    @Test
    fun `Uses localized client error for valid decline code, Locale is not US`() {
        localeTestRule.setTemporarily(Locale.FRANCE)
        runStripeErrorMappingTest(
            code = null,
            declineCode = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Votre moyen de paiement a été refusé.")
        }
    }

    @Test
    fun `Uses localized client error for card_declined code`() {
        runStripeErrorMappingTest(
            message = null,
            type = "card_error",
            code = "card_declined",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your card was declined")
        }
    }

    @Test
    fun `Uses original error message with invalid error codes`() {
        runStripeErrorMappingTest(
            message = "Backend message",
            code = "some_code_ive_never_encountered",
            declineCode = "some_code_ive_never_encountered",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("Backend message")
        }
    }

    @Test
    fun `Uses original error message`() {
        runStripeErrorMappingTest(
            message = "Backend message",
            code = null,
            declineCode = null,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("Backend message")
        }
    }

    @Test
    fun `Falls back to unexpected error message`() {
        runStripeErrorMappingTest(
            message = null,
            code = null,
            declineCode = null,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("There was an unexpected error -- try again in a few seconds")
        }
    }

    @Test
    fun `In live mode with no message falls back to message with request ID`() {
        runStripeErrorMappingTest(
            requestId = "req_abc123",
            message = null,
            type = "card_error",
            isLiveMode = true,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("Something went wrong. Request ID: req_abc123")
        }
    }

    @Test
    fun `In live mode with no message and no requestId falls back to unexpected error`() {
        runStripeErrorMappingTest(
            requestId = null,
            message = null,
            type = "card_error",
            isLiveMode = true,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("There was an unexpected error -- try again in a few seconds")
        }
    }

    @Test
    fun `In test mode, when type is not card_error code mapping is skipped and original message used in test mode`() {
        runStripeErrorMappingTest(
            type = "api_error",
            message = "Server error",
            code = "api_error",
            declineCode = null,
            isLiveMode = false,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("Server error")
        }
    }

    @Test
    fun `In live mode with original message set, requestId message is used when requestId is set`() {
        runStripeErrorMappingTest(
            requestId = "req_abc123",
            message = "Server error",
            type = "api_error",
            code = "api_error",
            declineCode = null,
            isLiveMode = true,
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage)
                .isEqualTo("Something went wrong. Request ID: req_abc123")
        }
    }

    private fun runStripeErrorMappingTest(
        message: String? = null,
        code: String? = null,
        declineCode: String? = null,
        type: String? = null,
        context: Context,
        requestId: String? = null,
        isLiveMode: Boolean = false,
        block: (String?) -> Unit,
    ) {
        val stripeError = StripeError(
            type = type,
            message = message,
            code = code,
            declineCode = declineCode
        )

        val paymentIntentError = PaymentIntent.Error(
            message = message,
            type = PaymentIntent.Error.Type.CardError,
            code = code,
            declineCode = declineCode,
            charge = null,
            docUrl = null,
            param = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        val setupIntentError = SetupIntent.Error(
            message = message,
            type = SetupIntent.Error.Type.CardError,
            code = code,
            declineCode = declineCode,
            docUrl = null,
            param = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        val localizedContext = localeTestRule.contextForLocale(context)

        block(
            stripeError.withLocalizedMessage(
                localizedContext,
                requestId,
                isLiveMode
            ).message
        )

        block(
            paymentIntentError.withLocalizedMessage(
                localizedContext,
                requestId,
                isLiveMode
            ).message
        )

        block(
            setupIntentError.withLocalizedMessage(
                localizedContext,
                requestId,
                isLiveMode
            ).message
        )
    }
}

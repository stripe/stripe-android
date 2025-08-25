package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.withLocalizedMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StripeErrorMappingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val backendMessage = "The backend message"

    @Test
    fun `Uses backend message for card error `() {
        runStripeErrorMappingTest(
            code = null,
            declineCode = null,
            context = context
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo(backendMessage)
        }
    }

    @Test
    fun `Uses localized client error for error code`() {
        runStripeErrorMappingTest(
            message = null,
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

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

    private fun runStripeErrorMappingTest(
        message: String? = backendMessage,
        code: String? = null,
        declineCode: String? = null,
        context: Context,
        block: (String?) -> Unit,
    ) {
        val stripeError = StripeError(
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

        withLocale(Locale.US) {
            block(stripeError.withLocalizedMessage(context).message)

            block(paymentIntentError.withLocalizedMessage(context).message)

            block(setupIntentError.withLocalizedMessage(context).message)
        }
    }

    private inline fun <T> withLocale(locale: Locale, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(locale)
        val result = block()
        Locale.setDefault(original)
        return result
    }
}

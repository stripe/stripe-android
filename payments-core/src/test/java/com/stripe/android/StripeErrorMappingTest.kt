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
            isCardError = true,
            context = context
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo(backendMessage)
        }
    }

    @Test
    fun `Uses localized client error when no backend message`() {
        runStripeErrorMappingTest(
            message = null,
            isCardError = true,
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

    @Test
    fun `Uses localized client error for non card error with valid error code`() {
        runStripeErrorMappingTest(
            isCardError = false,
            code = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

    @Test
    fun `Uses localized client error for non card error with valid decline code`() {
        runStripeErrorMappingTest(
            isCardError = false,
            code = null,
            declineCode = "generic_decline",
            context = context,
        ) { actualMessage ->
            assertThat(actualMessage).isEqualTo("Your payment method was declined.")
        }
    }

    @Test
    fun `Uses unexpected error for StripeError`() {
        runStripeErrorMappingTest(
            isCardError = false,
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
        isCardError: Boolean,
        code: String? = null,
        declineCode: String? = null,
        context: Context,
        block: (String?) -> Unit,
    ) {
        val stripeError = StripeError(
            message = message,
            type = if (isCardError) "card_error" else "api_error",
            code = code,
            declineCode = declineCode
        )

        val paymentIntentError = PaymentIntent.Error(
            message = message,
            type = if (isCardError) PaymentIntent.Error.Type.CardError else PaymentIntent.Error.Type.ApiError,
            code = code,
            declineCode = declineCode,
            charge = null,
            docUrl = null,
            param = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        val setupIntentError = SetupIntent.Error(
            message = message,
            type = if (isCardError) SetupIntent.Error.Type.CardError else SetupIntent.Error.Type.ApiError,
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

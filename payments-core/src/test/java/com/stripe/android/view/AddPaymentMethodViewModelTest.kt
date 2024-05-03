package com.stripe.android.view

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.Stripe
import com.stripe.android.analytics.PaymentSessionEventReporter
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val stripe = Stripe(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

    private val customerSession: CustomerSession = mock()

    @Test
    fun `on form shown, should fire onFormShown event`() {
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
            args = AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Fpx)
                .build()
        )

        viewModel.onFormShown()

        verify(eventReporter).onFormShown("fpx")
    }

    @Test
    fun `on form interacted, should fire onFormInteracted event`() {
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
            args = AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Fpx)
                .build()
        )

        viewModel.onFormInteracted()

        verify(eventReporter).onFormInteracted("fpx")
    }

    @Test
    fun `on card number completed, should fire onCardNumberCompleted event`() {
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
            args = AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Fpx)
                .build()
        )

        viewModel.onCardNumberCompleted()

        verify(eventReporter).onCardNumberCompleted()
    }

    @Test
    fun `on save, should fire onDoneButtonTapped event`() {
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
            args = AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Fpx)
                .build()
        )

        viewModel.onSaveClicked()

        verify(eventReporter).onDoneButtonTapped("fpx")
    }

    @Test
    fun `updatedPaymentMethodCreateParams should include expected attribution`() {
        val params = PaymentMethodCreateParams.create(
            PaymentMethodCreateParamsFixtures.CARD.copy(
                attribution = setOf("CardMultilineWidget")
            )
        )
        assertThat(
            createViewModel().updatedPaymentMethodCreateParams(params).attribution
        ).containsExactly(
            "CardMultilineWidget",
            AddPaymentMethodActivity.PRODUCT_TOKEN
        )
    }

    @Test
    fun attachPaymentMethod_whenError_returnsError() = runTest {
        whenever(
            customerSession.attachPaymentMethod(
                paymentMethodId = any(),
                productUsage = any(),
                listener = any()
            )
        ).thenAnswer { invocation ->
            val listener = invocation.arguments[2] as CustomerSession.PaymentMethodRetrievalListener
            listener.onError(
                402,
                ERROR_MESSAGE,
                StripeError(
                    code = "incorrect_cvc",
                    docUrl = "https://stripe.com/docs/error-codes/incorrect-cvc",
                    message = ERROR_MESSAGE,
                    param = "cvc",
                    type = "card_error"
                )
            )
        }

        val result = createViewModel().attachPaymentMethod(
            customerSession,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        verify(customerSession).attachPaymentMethod(
            eq("pm_123456789"),
            eq(EXPECTED_PRODUCT_USAGE),
            any()
        )

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun attachPaymentMethod_withCustomErrorMessageTranslator_whenError_returnsLocalizedError() = runTest {
        whenever(
            customerSession.attachPaymentMethod(
                paymentMethodId = any(),
                productUsage = any(),
                listener = any()
            )
        ).thenAnswer { invocation ->
            val listener = invocation.arguments[2] as CustomerSession.PaymentMethodRetrievalListener
            listener.onError(
                402,
                ERROR_MESSAGE,
                StripeError(
                    code = "incorrect_cvc",
                    docUrl = "https://stripe.com/docs/error-codes/incorrect-cvc",
                    message = ERROR_MESSAGE,
                    param = "cvc",
                    type = "card_error"
                )
            )
        }

        val result = createViewModel(translator = TRANSLATOR).attachPaymentMethod(
            customerSession,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        verify(customerSession).attachPaymentMethod(
            eq("pm_123456789"),
            eq(EXPECTED_PRODUCT_USAGE),
            any()
        )

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo(ERROR_MESSAGE_LOCALIZED)
    }

    private fun createViewModel(
        eventReporter: PaymentSessionEventReporter = mock(),
        args: AddPaymentMethodActivityStarter.Args = AddPaymentMethodActivityStarter.Args.Builder().build(),
        translator: ErrorMessageTranslator = TranslatorManager.getErrorMessageTranslator()
    ): AddPaymentMethodViewModel {
        return AddPaymentMethodViewModel(
            ApplicationProvider.getApplicationContext(),
            SavedStateHandle(),
            stripe,
            args,
            translator,
            eventReporter
        )
    }

    private companion object {
        private const val ERROR_MESSAGE = "Your card's security code is incorrect."
        private const val ERROR_MESSAGE_LOCALIZED =
            "El código de seguridad de la tarjeta es incorrecto."

        private val TRANSLATOR = object : ErrorMessageTranslator {
            override fun translate(
                httpCode: Int,
                errorMessage: String?,
                stripeError: StripeError?
            ): String {
                return if (stripeError?.code == "incorrect_cvc") {
                    ERROR_MESSAGE_LOCALIZED
                } else {
                    errorMessage.orEmpty()
                }
            }
        }

        private val EXPECTED_PRODUCT_USAGE = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN
        )
    }
}

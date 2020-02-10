package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodViewModelTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

    private val customerSession: CustomerSession = mock()
    private val paymentMethodRetrievalCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> = argumentCaptor()
    private val paymentMethodCreateParamsCaptor: KArgumentCaptor<PaymentMethodCreateParams> = argumentCaptor()

    @Test
    fun createPaymentMethod_shouldIncludeProductUsageTokens() {
        val stripe: Stripe = mock()
        createViewModel(
            stripe = stripe
        ).createPaymentMethod(
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParamsFixtures.CARD.copy(
                    attribution = setOf("CardMultilineWidget")
                )
            )
        )

        verify(stripe).createPaymentMethod(
            paymentMethodCreateParamsCaptor.capture(),
            anyOrNull(),
            any()
        )

        assertEquals(
            setOf("CardMultilineWidget", AddPaymentMethodActivity.PRODUCT_TOKEN),
            paymentMethodCreateParamsCaptor.firstValue.attribution
        )
    }

    @Test
    fun attachPaymentMethod_whenError_returnsError() {
        val resultData =
            createViewModel()
                .attachPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        verify(customerSession).attachPaymentMethod(
            eq("pm_123456789"),
            eq(EXPECTED_PRODUCT_USAGE),
            paymentMethodRetrievalCaptor.capture()
        )

        paymentMethodRetrievalCaptor.firstValue.onError(
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

        val errorResult =
            resultData.value as AddPaymentMethodViewModel.PaymentMethodResult.Error
        assertEquals(ERROR_MESSAGE, errorResult.errorMessage)
    }

    @Test
    fun attachPaymentMethod_withCustomErrorMessageTranslator_whenError_returnsLocalizedError() {
        val resultData =
            createViewModel(translator = TRANSLATOR)
                .attachPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        verify(customerSession).attachPaymentMethod(
            eq("pm_123456789"),
            eq(EXPECTED_PRODUCT_USAGE),
            paymentMethodRetrievalCaptor.capture()
        )

        paymentMethodRetrievalCaptor.firstValue.onError(
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

        val errorResult =
            resultData.value as AddPaymentMethodViewModel.PaymentMethodResult.Error
        assertEquals(ERROR_MESSAGE_LOCALIZED, errorResult.errorMessage)
    }

    private fun createViewModel(
        stripe: Stripe = Stripe(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
        translator: ErrorMessageTranslator = TranslatorManager.getErrorMessageTranslator()
    ): AddPaymentMethodViewModel {
        return AddPaymentMethodViewModel(
            stripe,
            customerSession,
            AddPaymentMethodActivityStarter.Args.DEFAULT,
            translator
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

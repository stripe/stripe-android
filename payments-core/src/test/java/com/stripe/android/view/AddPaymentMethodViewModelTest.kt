package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.Stripe
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val stripe = Stripe(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

    private val customerSession: CustomerSession = mock()
    private val paymentMethodRetrievalCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> = argumentCaptor()

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
    fun attachPaymentMethod_whenError_returnsError() {
        var throwable: Throwable? = null
        createViewModel()
            .attachPaymentMethod(
                customerSession,
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            ).observeForever {
                throwable = it.exceptionOrNull()
            }

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

        assertThat(throwable?.message)
            .isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun attachPaymentMethod_withCustomErrorMessageTranslator_whenError_returnsLocalizedError() {
        var throwable: Throwable? = null
        createViewModel(translator = TRANSLATOR)
            .attachPaymentMethod(
                customerSession,
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            ).observeForever {
                throwable = it.exceptionOrNull()
            }

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

        assertThat(throwable?.message)
            .isEqualTo(ERROR_MESSAGE_LOCALIZED)
    }

    private fun createViewModel(
        translator: ErrorMessageTranslator = TranslatorManager.getErrorMessageTranslator()
    ): AddPaymentMethodViewModel {
        return AddPaymentMethodViewModel(
            stripe,
            AddPaymentMethodActivityStarter.Args.Builder().build(),
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

package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodViewModelTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

    private val stripe: Stripe by lazy {
        Stripe(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    private val customerSession: CustomerSession = mock()
    private val paymentMethodRetrievalCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> = argumentCaptor()

    @Test
    fun attachPaymentMethod_whenError_returnsError() {
        val viewModel = AddPaymentMethodViewModel(
            stripe,
            customerSession,
            AddPaymentMethodActivityStarter.Args.DEFAULT
        )

        val resultData =
            viewModel.attachPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        verify(customerSession).attachPaymentMethod(
            eq("pm_123456789"),
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

    private companion object {
        private const val ERROR_MESSAGE = "Your card's security code is incorrect."
    }
}

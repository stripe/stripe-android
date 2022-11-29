package com.stripe.android.paymentmethodmessaging

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingView
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodMessagingViewModelTest {
    private val stripeApiRepository = mock<StripeApiRepository>()

    @Test
    fun `when valid message, loadMessage returns success`() = runTest {
        val viewModel = PaymentMethodMessagingViewModel(
            isSystemDarkTheme = false,
            configuration = PaymentMethodMessagingView.Configuration(
                publishableKey = "publishableKey",
                paymentMethods = setOf(),
                currency = "currency",
                amount = 999,
                imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
            ),
            stripeApiRepository = stripeApiRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "some html",
            learnMoreUrl = "some url"
        )

        val message = viewModel.loadMessage()

        assertThat(message.isSuccess).isTrue()
        assertThat(message.getOrNull()?.displayHtml)
            .isEqualTo("some html")
        assertThat(message.getOrNull()?.learnMoreUrl)
            .isEqualTo("some url")
    }

    @Test
    fun `when html is empty, loadMessage returns failure`() = runTest {
        val viewModel = PaymentMethodMessagingViewModel(
            isSystemDarkTheme = false,
            configuration = PaymentMethodMessagingView.Configuration(
                publishableKey = "publishableKey",
                paymentMethods = setOf(),
                currency = "currency",
                amount = 999,
                imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
            ),
            stripeApiRepository = stripeApiRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "",
            learnMoreUrl = "a"
        )

        val message = viewModel.loadMessage()

        assertThat(message.isFailure).isTrue()
        assertThat(message.exceptionOrNull()?.message)
            .isEqualTo("Could not retrieve message")
    }

    @Test
    fun `when url is empty, loadMessage returns failure`() = runTest {
        val viewModel = PaymentMethodMessagingViewModel(
            isSystemDarkTheme = false,
            configuration = PaymentMethodMessagingView.Configuration(
                publishableKey = "publishableKey",
                paymentMethods = setOf(),
                currency = "currency",
                amount = 999,
                imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
            ),
            stripeApiRepository = stripeApiRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "a",
            learnMoreUrl = ""
        )

        val message = viewModel.loadMessage()

        assertThat(message.isFailure).isTrue()
        assertThat(message.exceptionOrNull()?.message)
            .isEqualTo("Could not retrieve message")
    }

    private suspend fun returnsPaymentMethodMessage(
        displayHtml: String,
        learnMoreUrl: String
    ) {
        val paymentMethodMessage = mock<PaymentMethodMessage>()
        whenever(paymentMethodMessage.displayHtml).thenReturn(displayHtml)
        whenever(paymentMethodMessage.learnMoreUrl).thenReturn(learnMoreUrl)
        whenever(
            stripeApiRepository.retrievePaymentMethodMessage(
                paymentMethods = anyOrNull(),
                amount = anyOrNull(),
                currency = anyOrNull(),
                country = anyOrNull(),
                locale = anyOrNull(),
                logoColor = anyOrNull(),
                requestOptions = anyOrNull()
            )
        ).thenReturn(
            paymentMethodMessage
        )
    }
}

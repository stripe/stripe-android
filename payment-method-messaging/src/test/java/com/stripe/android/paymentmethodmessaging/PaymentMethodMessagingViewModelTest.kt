package com.stripe.android.paymentmethodmessaging

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingData
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingView
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodMessagingViewModelTest {

    private val stripeRepository = mock<StripeRepository>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when valid message, loadMessage returns success`() = runTest(UnconfinedTestDispatcher()) {
        val config = PaymentMethodMessagingView.Configuration(
            publishableKey = "publishableKey",
            paymentMethods = setOf(),
            currency = "currency",
            amount = 999,
            imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
        )

        val viewModel = PaymentMethodMessagingViewModel(
            mapper = { scope, message ->
                scope.async {
                    PaymentMethodMessagingData(
                        message = message,
                        images = mapOf(),
                        config = config
                    )
                }
            },
            isSystemDarkThemeProvider = { false },
            config = config,
            stripeRepository = stripeRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "some html",
            learnMoreUrl = "some url"
        )

        assertThat(viewModel.messageFlow.stateIn(viewModel.viewModelScope).value).isNull()

        viewModel.loadMessage()

        val result = viewModel.messageFlow.stateIn(viewModel.viewModelScope).value!!

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.message?.displayHtml)
            .isEqualTo("some html")
        assertThat(result.getOrNull()?.message?.learnMoreUrl)
            .isEqualTo("some url")
    }

    @Test
    fun `when html is empty, loadMessage returns failure`() = runTest {
        val config = PaymentMethodMessagingView.Configuration(
            publishableKey = "publishableKey",
            paymentMethods = setOf(),
            currency = "currency",
            amount = 999,
            imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
        )

        val viewModel = PaymentMethodMessagingViewModel(
            mapper = { scope, _ ->
                scope.async { mock() }
            },
            isSystemDarkThemeProvider = { false },
            config = config,
            stripeRepository = stripeRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "",
            learnMoreUrl = "a"
        )

        assertThat(viewModel.messageFlow.stateIn(viewModel.viewModelScope).value).isNull()

        viewModel.loadMessage()

        val result = viewModel.messageFlow.stateIn(viewModel.viewModelScope).value!!

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Could not retrieve message")
    }

    @Test
    fun `when url is empty, loadMessage returns failure`() = runTest {
        val config = PaymentMethodMessagingView.Configuration(
            publishableKey = "publishableKey",
            paymentMethods = setOf(),
            currency = "currency",
            amount = 999,
            imageColor = PaymentMethodMessagingView.Configuration.ImageColor.Color
        )

        val viewModel = PaymentMethodMessagingViewModel(
            mapper = { scope, _ ->
                scope.async { mock() }
            },
            isSystemDarkThemeProvider = { false },
            config = config,
            stripeRepository = stripeRepository
        )

        returnsPaymentMethodMessage(
            displayHtml = "a",
            learnMoreUrl = ""
        )

        assertThat(viewModel.messageFlow.stateIn(viewModel.viewModelScope).value).isNull()

        viewModel.loadMessage()

        val result = viewModel.messageFlow.stateIn(viewModel.viewModelScope).value!!

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Could not retrieve message")
    }

    private suspend fun returnsPaymentMethodMessage(
        displayHtml: String,
        learnMoreUrl: String
    ) {
        val paymentMethodMessage = PaymentMethodMessage(
            displayHtml = displayHtml,
            learnMoreUrl = learnMoreUrl,
        )

        whenever(
            stripeRepository.retrievePaymentMethodMessage(
                paymentMethods = anyOrNull(),
                amount = anyOrNull(),
                currency = anyOrNull(),
                country = anyOrNull(),
                locale = anyOrNull(),
                logoColor = anyOrNull(),
                requestOptions = anyOrNull()
            )
        ).thenReturn(
            Result.success(paymentMethodMessage)
        )
    }
}

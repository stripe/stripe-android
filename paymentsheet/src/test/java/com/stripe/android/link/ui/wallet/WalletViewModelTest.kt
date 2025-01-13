package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `viewmodel should load payment methods on init`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var paymentMethodTypes: Set<String>? = null
            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                this.paymentMethodTypes = paymentMethodTypes
                return super.listPaymentDetails(paymentMethodTypes)
            }
        }

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager
        )

        assertThat(linkAccountManager.paymentMethodTypes)
            .containsExactlyElementsIn(TestFactory.LINK_CONFIGURATION.stripeIntent.paymentMethodTypes)

        assertThat(viewModel.uiState.value).isEqualTo(
            WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
                isProcessing = false,
                hasCompleted = false,
                primaryButtonLabel = TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL
            )
        )
    }

    @Test
    fun `viewmodel should dismiss with failure on load payment method failure`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.failure(error)

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val logger = FakeLogger()

        createViewModel(
            linkAccountManager = linkAccountManager,
            logger = logger,
            dismissWithResult = ::dismissWithResult
        )

        assertThat(linkActivityResult).isEqualTo(LinkActivityResult.Failed(error))
        assertThat(logger.errorLogs).isEqualTo(listOf("WalletViewModel Fatal error: " to error))
    }

    @Test
    fun `viewmodel should open payment method screen when none is available`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(ConsumerPaymentDetails(emptyList()))

        var navScreen: LinkScreen? = null
        fun navigateAndClearStack(screen: LinkScreen) {
            navScreen = screen
        }

        createViewModel(
            linkAccountManager = linkAccountManager,
            navigateAndClearStack = ::navigateAndClearStack
        )

        assertThat(navScreen).isEqualTo(LinkScreen.PaymentMethod)
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        logger: Logger = FakeLogger(),
        navigateAndClearStack: (route: LinkScreen) -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): WalletViewModel {
        return WalletViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            logger = logger,
            navigateAndClearStack = navigateAndClearStack,
            dismissWithResult = dismissWithResult
        )
    }
}

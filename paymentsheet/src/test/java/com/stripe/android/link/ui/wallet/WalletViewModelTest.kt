package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.Result
import kotlin.String
import kotlin.Throwable
import kotlin.Unit
import kotlin.time.Duration.Companion.seconds
import kotlin.to
import com.stripe.android.link.confirmation.Result as ConfirmationResult

@RunWith(RobolectricTestRunner::class)
class WalletViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
                primaryButtonLabel = TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL,
                errorMessage = null
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

    @Test
    fun `viewmodel should dismiss link after successful payment`() = runTest(dispatcher) {
        val linkConfirmationHandler = object : FakeLinkConfirmationHandler() {
            override suspend fun confirm(
                paymentDetails: ConsumerPaymentDetails.PaymentDetails,
                linkAccount: LinkAccount
            ): com.stripe.android.link.confirmation.Result {
                delay(1.seconds)
                return super.confirm(paymentDetails, linkAccount)
            }
        }
        linkConfirmationHandler.result = ConfirmationResult.Succeeded

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val vm = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = ::dismissWithResult
        )

        vm.onPrimaryButtonClicked()

        assertThat(vm.uiState.value.isProcessing).isTrue()

        dispatcher.scheduler.advanceTimeBy(1.5.seconds)

        assertThat(linkActivityResult).isEqualTo(LinkActivityResult.Completed)
        assertThat(vm.uiState.value.errorMessage).isEqualTo(null)
    }

    @Test
    fun `viewmodel should display error after failed payment`() = runTest(dispatcher) {
        val errorMessage = "Something's up".resolvableString
        val linkConfirmationHandler = object : FakeLinkConfirmationHandler() {
            override suspend fun confirm(
                paymentDetails: ConsumerPaymentDetails.PaymentDetails,
                linkAccount: LinkAccount
            ): com.stripe.android.link.confirmation.Result {
                delay(1.seconds)
                return ConfirmationResult.Failed(errorMessage)
            }
        }

        val vm = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler
        )

        vm.onPrimaryButtonClicked()

        assertThat(vm.uiState.value.isProcessing).isTrue()

        dispatcher.scheduler.advanceTimeBy(1.5.seconds)

        assertThat(vm.uiState.value.errorMessage).isEqualTo(errorMessage)
        assertThat(vm.uiState.value.isProcessing).isFalse()
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        logger: Logger = FakeLogger(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        navigateAndClearStack: (route: LinkScreen) -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): WalletViewModel {
        return WalletViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            logger = logger,
            linkConfirmationHandler = linkConfirmationHandler,
            navigateAndClearStack = navigateAndClearStack,
            dismissWithResult = dismissWithResult
        )
    }
}

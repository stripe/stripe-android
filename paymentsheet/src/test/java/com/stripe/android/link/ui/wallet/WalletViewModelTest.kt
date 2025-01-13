package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.Result
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable
import kotlin.Unit
import kotlin.to
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

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
                primaryButtonLabel = TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL,
                expiryDateInput = FormFieldEntry(""),
                cvcInput = FormFieldEntry("")
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
    fun `viewmodel should open card edit screen when onEditPaymentMethodClicked`() = runTest(dispatcher) {
        var navScreen: LinkScreen? = null
        fun navigate(screen: LinkScreen) {
            navScreen = screen
        }

        val vm = createViewModel(navigate = ::navigate)

        vm.onEditPaymentMethodClicked(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        assertThat(navScreen).isEqualTo(LinkScreen.CardEdit)
    }

    @Test
    fun `viewmodel should open payment method screen when onAddNewPaymentMethodClicked`() = runTest(dispatcher) {
        var navScreen: LinkScreen? = null
        fun navigate(screen: LinkScreen) {
            navScreen = screen
        }

        val vm = createViewModel(navigate = ::navigate)

        vm.onAddNewPaymentMethodClicked()

        assertThat(navScreen).isEqualTo(LinkScreen.PaymentMethod)
    }

    fun `expiryDateController updates uiState when input changes`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.expiryDateController.onRawValueChange("12")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.expiryDateInput).isEqualTo(FormFieldEntry("12", isComplete = false))

        viewModel.expiryDateController.onRawValueChange("12/25")
        assertThat(viewModel.uiState.value.expiryDateInput).isEqualTo(FormFieldEntry("1225", isComplete = true))
    }

    @Test
    fun `cvcController updates uiState when input changes`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.cvcController.onRawValueChange("12")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.cvcInput).isEqualTo(FormFieldEntry("12", isComplete = false))

        viewModel.cvcController.onRawValueChange("123")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.cvcInput).isEqualTo(FormFieldEntry("123", isComplete = true))
    }

    @Test
    fun `expiryDateController and cvcController reset when new item is selected`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.expiryDateController.onRawValueChange("12/25")
        viewModel.cvcController.onRawValueChange("123")

        assertThat(viewModel.uiState.value.expiryDateInput).isEqualTo(FormFieldEntry("1225", isComplete = true))
        assertThat(viewModel.uiState.value.cvcInput).isEqualTo(FormFieldEntry("123", isComplete = true))

        val newCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "new_card_id")
        viewModel.onItemSelected(newCard)

        assertThat(viewModel.uiState.value.expiryDateInput).isEqualTo(FormFieldEntry("", isComplete = false))
        assertThat(viewModel.uiState.value.cvcInput).isEqualTo(FormFieldEntry("", isComplete = false))
    }

    @Test
    fun `performPaymentConfirmation updates expired card successfully`() = runTest(dispatcher) {
        val expiredCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1999)
        val updatedCard = expiredCard.copy(expiryYear = 2099)
        val linkAccountManager = object : FakeLinkAccountManager() {
            var updateParamsUsed: ConsumerPaymentDetailsUpdateParams? = null
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams
            ): Result<ConsumerPaymentDetails> {
                updateParamsUsed = updateParams
                return Result.success(
                    ConsumerPaymentDetails(paymentDetails = listOf(updatedCard))
                )
            }
        }
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(expiredCard))
        )
        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler
        )
        viewModel.onItemSelected(expiredCard)
        viewModel.expiryDateController.onRawValueChange("1299")
        viewModel.cvcController.onRawValueChange("123")

        viewModel.onPrimaryButtonClicked()

        assertThat(viewModel.uiState.value.isProcessing).isTrue()
        assertThat(viewModel.uiState.value.alertMessage).isNull()

        assertThat(linkAccountManager.updateParamsUsed?.id).isEqualTo(expiredCard.id)
        val card = linkAccountManager.updateParamsUsed?.cardPaymentMethodCreateParamsMap
            ?.get("card") as? Map<*, *>
        assertThat(card).isEqualTo(
            mapOf(
                "exp_month" to updatedCard.expiryMonth.toString(),
                "exp_year" to updatedCard.expiryYear.toString()
            )
        )

        assertThat(linkConfirmationHandler.calls).containsExactly(
            FakeLinkConfirmationHandler.Call(
                paymentDetails = updatedCard,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = "123"
            )
        )
    }

    @Test
    fun `performPaymentConfirmation handles update failure`() = runTest(dispatcher) {
        val error = RuntimeException("Update failed")
        val expiredCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1999)
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(expiredCard))
        )
        linkAccountManager.updatePaymentDetailsResult = Result.failure(error)

        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler
        )
        viewModel.onItemSelected(expiredCard)
        viewModel.expiryDateController.onRawValueChange("1225")
        viewModel.cvcController.onRawValueChange("123")

        viewModel.onPrimaryButtonClicked()

        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(viewModel.uiState.value.alertMessage).isEqualTo(error.stripeErrorMessage())

        assertThat(linkConfirmationHandler.calls).isEmpty()
    }

    @Test
    fun `performPaymentConfirmation skips update for non-expired card`() = runTest(dispatcher) {
        val validCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099)
        val linkAccountManager = object : FakeLinkAccountManager() {
            var updatePaymentDetailsCalls = 0
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams
            ): Result<ConsumerPaymentDetails> {
                updatePaymentDetailsCalls += 1
                return super.updatePaymentDetails(updateParams)
            }

            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                return Result.success(
                    value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
                )
            }
        }

        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler
        )
        viewModel.onItemSelected(validCard)

        viewModel.onPrimaryButtonClicked()

        assertThat(linkAccountManager.updatePaymentDetailsCalls).isEqualTo(0)

        assertThat(linkConfirmationHandler.calls).containsExactly(
            FakeLinkConfirmationHandler.Call(
                paymentDetails = validCard,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = null
            )
        )
    }

    @Test
    fun `performPaymentConfirmation dismisses with Completed result on success`() = runTest(dispatcher) {
        val validCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099)
        val linkAccountManager = object : FakeLinkAccountManager() {
            var updatePaymentDetailsCalls = 0
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams
            ): Result<ConsumerPaymentDetails> {
                updatePaymentDetailsCalls += 1
                return super.updatePaymentDetails(updateParams)
            }

            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                return Result.success(
                    value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
                )
            }
        }

        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = {
                result = it
            }
        )
        viewModel.onItemSelected(validCard)

        viewModel.onPrimaryButtonClicked()

        assertThat(linkAccountManager.updatePaymentDetailsCalls).isEqualTo(0)

        assertThat(linkConfirmationHandler.calls).containsExactly(
            FakeLinkConfirmationHandler.Call(
                paymentDetails = validCard,
                cvc = null,
                linkAccount = TestFactory.LINK_ACCOUNT
            )
        )

        assertThat(result).isEqualTo(LinkActivityResult.Completed)
    }

    @Test
    fun `performPaymentConfirmation displays error on failure result`() = runTest(dispatcher) {
        val confirmationResult = LinkConfirmationResult.Failed("oops".resolvableString)
        val validCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099)
        val linkAccountManager = object : FakeLinkAccountManager() {
            var updatePaymentDetailsCalls = 0
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams
            ): Result<ConsumerPaymentDetails> {
                updatePaymentDetailsCalls += 1
                return super.updatePaymentDetails(updateParams)
            }

            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                return Result.success(
                    value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
                )
            }
        }

        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        linkConfirmationHandler.confirmResult = confirmationResult

        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = {
                result = it
            }
        )
        viewModel.onItemSelected(validCard)

        viewModel.onPrimaryButtonClicked()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(confirmationResult.message)
        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(result).isNull()
    }

    @Test
    fun `performPaymentConfirmation does nothing on canceled result`() = runTest(dispatcher) {
        val confirmationResult = LinkConfirmationResult.Canceled
        val validCard = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099)
        val linkAccountManager = object : FakeLinkAccountManager() {
            var updatePaymentDetailsCalls = 0
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams
            ): Result<ConsumerPaymentDetails> {
                updatePaymentDetailsCalls += 1
                return super.updatePaymentDetails(updateParams)
            }

            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                return Result.success(
                    value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
                )
            }
        }

        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        linkConfirmationHandler.confirmResult = confirmationResult

        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = {
                result = it
            }
        )
        viewModel.onItemSelected(validCard)

        viewModel.onPrimaryButtonClicked()

        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(result).isNull()
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        logger: Logger = FakeLogger(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        navigate: (route: LinkScreen) -> Unit = {},
        navigateAndClearStack: (route: LinkScreen) -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): WalletViewModel {
        return WalletViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            logger = logger,
            navigate = navigate,
            navigateAndClearStack = navigateAndClearStack,
            dismissWithResult = dismissWithResult
        )
    }
}

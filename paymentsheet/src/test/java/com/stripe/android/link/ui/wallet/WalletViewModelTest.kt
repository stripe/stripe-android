package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
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
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

@RunWith(RobolectricTestRunner::class)
class WalletViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `viewmodel should load payment methods on init`() = runTest(dispatcher) {
        val linkAccountManager = WalletLinkAccountManager()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager
        )

        assertThat(linkAccountManager.listPaymentDetailsCalls)
            .containsExactly(TestFactory.LINK_CONFIGURATION.stripeIntent.paymentMethodTypes.toSet())

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
        val linkAccountManager = WalletLinkAccountManager()
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
        val linkAccountManager = WalletLinkAccountManager()
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
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.updatePaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(updatedCard))
        )
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

        val updateParamsUsed = linkAccountManager.updatePaymentDetailsCalls.firstOrNull()
        val card = updateParamsUsed?.cardPaymentMethodCreateParamsMap
            ?.get("card") as? Map<*, *>

        assertThat(updateParamsUsed?.id).isEqualTo(expiredCard.id)
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
        val linkAccountManager = WalletLinkAccountManager()
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
        val linkAccountManager = WalletLinkAccountManager()

        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler
        )
        viewModel.onItemSelected(validCard)

        viewModel.onPrimaryButtonClicked()

        assertThat(linkAccountManager.updatePaymentDetailsCalls).isEmpty()

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
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
        )

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

        assertThat(linkAccountManager.updatePaymentDetailsCalls).isEmpty()

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
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
        )

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
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            value = ConsumerPaymentDetails(paymentDetails = listOf(validCard))
        )

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

    @Test
    fun `onSetDefaultClicked updates payment method as default successfully`() = runTest(dispatcher) {
        val card1 = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card1", isDefault = false)
        val card2 = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card2", isDefault = true)
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card1, card2))
        )

        val updatedCard1 = card1.copy(isDefault = true)
        val updatedCard2 = card2.copy(isDefault = false)
        linkAccountManager.updatePaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(updatedCard1))
        )

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(updatedCard1, updatedCard2))
        )

        viewModel.onSetDefaultClicked(card1)

        assertThat(linkAccountManager.updatePaymentDetailsCalls).containsExactly(
            ConsumerPaymentDetailsUpdateParams(
                id = "card1",
                isDefault = true,
                cardPaymentMethodCreateParamsMap = null
            )
        )

        assertThat(viewModel.uiState.value.paymentDetailsList).containsExactly(updatedCard1, updatedCard2)
        assertThat(linkAccountManager.listPaymentDetailsCalls.size).isEqualTo(2)
        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(viewModel.uiState.value.alertMessage).isNull()
    }

    @Test
    fun `onSetDefaultClicked handles update failure`() = runTest(dispatcher) {
        val error = RuntimeException("Update failed")
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card1", isDefault = false)
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card))
        )
        linkAccountManager.updatePaymentDetailsResult = Result.failure(error)

        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            logger = logger
        )

        viewModel.onSetDefaultClicked(card)

        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(viewModel.uiState.value.alertMessage).isEqualTo(error.stripeErrorMessage())
        assertThat(logger.errorLogs).contains("WalletViewModel: Failed to set payment method as default" to error)
    }

    @Test
    fun `onRemoveClicked deletes payment method successfully`() = runTest(dispatcher) {
        val card1 = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card1")
        val card2 = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card2")
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card1, card2))
        )

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        advanceUntilIdle()

        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card2))
        )

        viewModel.onRemoveClicked(card1)

        assertThat(linkAccountManager.deletePaymentDetailsCalls).containsExactly("card1")

        // Verify that loadPaymentDetails is called after successful deletion
        assertThat(linkAccountManager.listPaymentDetailsCalls.size).isEqualTo(2)
        assertThat(viewModel.uiState.value.paymentDetailsList).containsExactly(card2)
    }

    @Test
    fun `onRemoveClicked handles deletion failure`() = runTest(dispatcher) {
        val error = RuntimeException("Deletion failed")
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "card1")
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card))
        )
        linkAccountManager.deletePaymentDetailsResult = Result.failure(error)

        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            logger = logger
        )

        viewModel.onRemoveClicked(card)

        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(viewModel.uiState.value.alertMessage).isEqualTo(error.stripeErrorMessage())
        assertThat(logger.errorLogs).contains("WalletViewModel: Failed to delete payment method" to error)
    }

    private fun createViewModel(
        linkAccountManager: WalletLinkAccountManager = WalletLinkAccountManager(),
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

private class WalletLinkAccountManager : FakeLinkAccountManager() {
    val listPaymentDetailsCalls = arrayListOf<Set<String>>()
    val updatePaymentDetailsCalls = arrayListOf<ConsumerPaymentDetailsUpdateParams>()
    val deletePaymentDetailsCalls = arrayListOf<String>()

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        listPaymentDetailsCalls.add(paymentMethodTypes)
        return super.listPaymentDetails(paymentMethodTypes)
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams
    ): Result<ConsumerPaymentDetails> {
        updatePaymentDetailsCalls.add(updateParams)
        return super.updatePaymentDetails(updateParams)
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit> {
        deletePaymentDetailsCalls.add(paymentDetailsId)
        return super.deletePaymentDetails(paymentDetailsId)
    }
}

package com.stripe.android.link.ui.wallet

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH
import com.stripe.android.link.TestFactory.CONSUMER_SHIPPING_ADDRESSES
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.Result
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

typealias CollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

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

        val state = viewModel.uiState.value

        assertThat(state).isEqualTo(
            WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                email = "email@stripe.com",
                selectedItemId = null,
                cardBrandFilter = TestFactory.LINK_CONFIGURATION.cardBrandFilter,
                isProcessing = false,
                hasCompleted = false,
                userSetIsExpanded = false,
                primaryButtonLabel = TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL,
                secondaryButtonLabel = TestFactory.LINK_WALLET_SECONDARY_BUTTON_LABEL,
                expiryDateInput = FormFieldEntry(""),
                cvcInput = FormFieldEntry(""),
                addPaymentMethodOptions = listOf(AddPaymentMethodOption.Card),
                isSettingUp = false,
                merchantName = "merchantName",
            )
        )
        assertThat(state.selectedItem).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull())
    }

    @Test
    fun `viewmodel should dismiss with failure on load payment method failure`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
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

        assertThat(linkActivityResult)
            .isEqualTo(
                LinkActivityResult.Failed(
                    error = error,
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
                )
            )
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
    fun `viewmodel should open payment method screen when add card option clicked`() = runTest(dispatcher) {
        val navigationManager = TestNavigationManager()
        val vm = createViewModel(navigationManager = navigationManager)

        vm.onAddPaymentMethodOptionClicked(AddPaymentMethodOption.Card)

        navigationManager.assertNavigatedTo(LinkScreen.PaymentMethod.route)
    }

    @Test
    fun `viewmodel should handle state updates through the add bank payment method happy path`() = runTest(dispatcher) {
        val linkAccount = TestFactory.LINK_ACCOUNT_WITH_PK
        val newBankAccountId = "pm_9872893"
        testAddBankAccount(
            linkAccount = linkAccount,
        ) { vm, linkAccountManager ->
            val newBankAccountDetails = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT.copy(id = newBankAccountId)
            linkAccountManager.createBankAccountPaymentDetailsResult = Result.success(newBankAccountDetails)

            awaitItem().run {
                assertThat(userSetIsExpanded).isTrue()
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
            }

            vm.onAddPaymentMethodOptionClicked(AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full))
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing())
            }
            awaitItem().run {
                val expectedConfig = FinancialConnectionsSheetConfiguration(
                    financialConnectionsSessionClientSecret = TestFactory.LINK_ACCOUNT_SESSION.clientSecret,
                    publishableKey = linkAccount.consumerPublishableKey!!,
                )
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing(expectedConfig))
            }

            vm.onPresentFinancialConnections(true)
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing())
            }

            vm.onFinancialConnectionsResult(
                FinancialConnectionsSheetResult.Completed(mockFinancialConnectionsSession())
            )
            awaitItem().run {
                assertThat(selectedItemId).isEqualTo(newBankAccountId)
                assertThat(userSetIsExpanded).isFalse()
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
            }
        }
    }

    @Test
    fun `viewmodel should handle link account session request error when adding bank account`() = runTest(dispatcher) {
        testAddBankAccount { vm, linkAccountManager ->
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
            }

            val error = Throwable("oops")
            linkAccountManager.createLinkAccountSessionResult = Result.failure(error)
            vm.onAddPaymentMethodOptionClicked(AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full))
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing())
            }
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
                assertThat(alertMessage).isEqualTo(error.stripeErrorMessage())
            }
        }
    }

    @Test
    fun `viewmodel should handle onFinancialConnectionsResult with Canceled`() = runTest(dispatcher) {
        testAddBankAccount { vm, _ ->
            skipItems(1)
            vm.onPresentFinancialConnections(true)
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing())
            }

            vm.onFinancialConnectionsResult(FinancialConnectionsSheetResult.Canceled)
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
            }
        }
    }

    @Test
    fun `viewmodel should handle onFinancialConnectionsResult with Failed`() = runTest(dispatcher) {
        testAddBankAccount { vm, _ ->
            skipItems(1)
            vm.onPresentFinancialConnections(true)
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Processing())
            }

            val error = Throwable("oops")
            vm.onFinancialConnectionsResult(FinancialConnectionsSheetResult.Failed(error))
            awaitItem().run {
                assertThat(addBankAccountState).isEqualTo(AddBankAccountState.Idle)
                assertThat(alertMessage).isEqualTo(error.stripeErrorMessage())
            }
        }
    }

    @Test
    fun `viewmodel should init with correct Add Payment Method options`() = runTest(dispatcher) {
        val account = TestFactory.LINK_ACCOUNT_WITH_PK
        val stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            linkFundingSources = listOf(
                ConsumerPaymentDetails.Card.TYPE,
                ConsumerPaymentDetails.BankAccount.TYPE,
            )
        )
        val configuration = TestFactory.LINK_CONFIGURATION.copy(stripeIntent = stripeIntent)

        assertThat(
            createViewModel(
                linkAccount = account,
                configuration = configuration,
            ).uiState.value.addPaymentMethodOptions
        ).containsExactly(
            AddPaymentMethodOption.Card,
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full),
        )

        assertThat(
            createViewModel(
                linkAccount = TestFactory.LINK_ACCOUNT,
                configuration = configuration,
            ).uiState.value.addPaymentMethodOptions
        ).containsExactly(
            AddPaymentMethodOption.Card,
        )

        assertThat(
            createViewModel(
                linkAccount = account,
                configuration = configuration.copy(financialConnectionsAvailability = null),
            ).uiState.value.addPaymentMethodOptions
        ).containsExactly(
            AddPaymentMethodOption.Card,
        )

        assertThat(
            createViewModel(
                linkAccount = account,
                configuration = configuration.copy(stripeIntent = stripeIntent.copy(linkFundingSources = emptyList()))
            ).uiState.value.addPaymentMethodOptions
        ).containsExactly(
            AddPaymentMethodOption.Card, // Card is available by default.
        )
    }

    @Test
    fun `viewModel should open update screen when onUpdateClicked`() = runTest(dispatcher) {
        val navigationManager = TestNavigationManager()
        val vm = createViewModel(navigationManager = navigationManager)

        vm.onUpdateClicked(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        navigationManager.assertNavigatedTo(
            route = LinkScreen.UpdateCard(
                paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
                billingDetailsUpdateFlow = null
            )
        )
    }

    @Test
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
    fun `selecting a valid payment method closes the payment method picker`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isExpanded).isFalse()

            viewModel.onExpandedChanged(expanded = true)

            val expandedState = awaitItem()
            assertThat(expandedState.isExpanded).isTrue()

            viewModel.onItemSelected(item = expandedState.paymentDetailsList.last())
            assertThat(awaitItem().isExpanded).isFalse()
        }
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
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                paymentDetails = listOf(
                    validCard,
                    CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                    CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
                )
            )
        )

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
        val account = TestFactory.LINK_ACCOUNT
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(account))
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

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(null, PaymentConfirmed),
                    selectedPayment = null
                )
            )
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
        val linkAccountManager = object : WalletLinkAccountManager() {
            override suspend fun updatePaymentDetails(
                updateParams: ConsumerPaymentDetailsUpdateParams,
                billingPhone: String?
            ): Result<ConsumerPaymentDetails> {
                delay(CARD_PROCESSING_DELAY)
                return super.updatePaymentDetails(updateParams, billingPhone)
            }
        }
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(card1, card2))
        )

        val updatedCard1 = card1.copy(isDefault = true)
        val updatedCard2 = card2.copy(isDefault = false)
        linkAccountManager.updatePaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(paymentDetails = listOf(updatedCard1))
        )

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        viewModel.onSetDefaultClicked(card1)

        assertThat(viewModel.uiState.value.cardBeingUpdated).isEqualTo(card1.id)

        dispatcher.scheduler.advanceTimeBy(CARD_PROCESSING_COMPLETION_TIME)

        assertThat(linkAccountManager.updatePaymentDetailsCalls).containsExactly(
            ConsumerPaymentDetailsUpdateParams(
                id = "card1",
                isDefault = true,
                cardPaymentMethodCreateParamsMap = null
            )
        )
        assertThat(viewModel.uiState.value.paymentDetailsList).containsExactly(updatedCard1, updatedCard2)
        assertThat(linkAccountManager.listPaymentDetailsCalls.size).isEqualTo(1)
        assertThat(viewModel.uiState.value.isProcessing).isFalse()
        assertThat(viewModel.uiState.value.cardBeingUpdated).isNull()
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
        assertThat(viewModel.uiState.value.cardBeingUpdated).isNull()
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

    @Test
    fun `onRemoveClicked shows inline indicator correctly`() = runTest(dispatcher) {
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

        val paymentMethodsBeingUpdated = viewModel.uiState.map { it.cardBeingUpdated }.distinctUntilChanged()

        paymentMethodsBeingUpdated.test {
            assertThat(awaitItem()).isNull()

            viewModel.onRemoveClicked(card)
            assertThat(awaitItem()).isEqualTo(card.id)

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `state respects card PMO SFU off session in passthrough mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Card.code,
                    sfuValue = "off_session"
                )
            ),
            passthroughModeEnabled = true
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isTrue()
    }

    @Test
    fun `state respects card PMO SFU on session in passthrough mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Card.code,
                    sfuValue = "on_session"
                )
            ),
            passthroughModeEnabled = true
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isTrue()
    }

    @Test
    fun `state does not respect card PMO SFU in payment method mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Card.code,
                    sfuValue = "off_session"
                )
            ),
            passthroughModeEnabled = false
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isFalse()
    }

    @Test
    fun `state does not respect link PMO SFU in passthrough mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Link.code,
                    sfuValue = "off_session"
                )
            ),
            passthroughModeEnabled = true
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isFalse()
    }

    @Test
    fun `state respects link PMO SFU off session in payment method mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Link.code,
                    sfuValue = "off_session"
                )
            ),
            passthroughModeEnabled = false
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isTrue()
    }

    @Test
    fun `state respects link PMO SFU on session in payment method mode`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = PaymentMethod.Type.Link.code,
                    sfuValue = "on_session"
                )
            ),
            passthroughModeEnabled = false
        )

        val viewModel = createViewModel(
            configuration = configuration
        )

        assertThat(viewModel.uiState.value.isSettingUp).isTrue()
    }

    @Test
    fun `Loads default shipping address in payment method selection mode`() = runTest(dispatcher) {
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
        linkAccountManager.listShippingAddressesResult = Result.success(CONSUMER_SHIPPING_ADDRESSES)

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            passthroughModeEnabled = false,
        )

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(
                selectedPayment = null,
            ),
            dismissWithResult = ::dismissWithResult,
        )

        viewModel.onPrimaryButtonClicked()

        val completedResult = linkActivityResult as? LinkActivityResult.Completed
        assertThat(completedResult?.shippingAddress).isEqualTo(CONSUMER_SHIPPING_ADDRESSES.addresses.first())
    }

    @Test
    fun `Does not load default shipping address if not payment method selection mode`() = runTest(dispatcher) {
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
        linkAccountManager.listShippingAddressesResult = Result.success(CONSUMER_SHIPPING_ADDRESSES)

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            passthroughModeEnabled = false,
        )

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            linkLaunchMode = LinkLaunchMode.Full,
            dismissWithResult = ::dismissWithResult,
        )

        viewModel.onPrimaryButtonClicked()

        val completedResult = linkActivityResult as? LinkActivityResult.Completed
        assertThat(completedResult?.shippingAddress).isNull()
    }

    @Test
    fun `Failure while loading default shipping address still allows completion`() = runTest(dispatcher) {
        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
        linkAccountManager.listShippingAddressesResult = Result.failure(APIConnectionException())

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            passthroughModeEnabled = false,
        )

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(
                selectedPayment = null,
            ),
            dismissWithResult = ::dismissWithResult,
        )

        viewModel.onPrimaryButtonClicked()

        val completedResult = linkActivityResult as? LinkActivityResult.Completed
        assertThat(completedResult?.shippingAddress).isNull()
    }

    private fun createViewModel(
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT,
        linkAccountManager: WalletLinkAccountManager = WalletLinkAccountManager(),
        navigationManager: NavigationManager = TestNavigationManager(),
        logger: Logger = FakeLogger(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        navigateAndClearStack: (route: LinkScreen) -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = {},
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full
    ): WalletViewModel {
        return WalletViewModel(
            configuration = configuration,
            linkAccount = linkAccount,
            linkAccountManager = linkAccountManager,
            logger = logger,
            navigateAndClearStack = navigateAndClearStack,
            dismissWithResult = dismissWithResult,
            navigationManager = navigationManager,
            dismissalCoordinator = dismissalCoordinator,
            completeLinkFlow = DefaultCompleteLinkFlow(
                linkConfirmationHandler = linkConfirmationHandler,
                linkAccountManager = linkAccountManager,
                dismissalCoordinator = dismissalCoordinator,
                linkLaunchMode = linkLaunchMode
            ),
            linkLaunchMode = linkLaunchMode
        )
    }

    private suspend fun testAddBankAccount(
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT_WITH_PK,
        validate: suspend TurbineTestContext<WalletUiState>.(WalletViewModel, WalletLinkAccountManager) -> Unit,
    ) {
        val linkAccountManager = WalletLinkAccountManager()
        val vm = createViewModel(
            linkAccount = linkAccount,
            linkAccountManager = linkAccountManager,
        )
        vm.onExpandedChanged(true)

        vm.uiState.test { validate(vm, linkAccountManager) }
    }

    private fun mockFinancialConnectionsSession(): FinancialConnectionsSession {
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(financialConnectionsSession.id).thenReturn("123")
        whenever(financialConnectionsSession.accounts).thenReturn(
            FinancialConnectionsAccountList(
                data = listOf(TestFactory.FINANCIAL_CONNECTIONS_CHECKING_ACCOUNT),
                hasMore = false,
                url = "url",
            )
        )
        return financialConnectionsSession
    }

    @Test
    fun `uses signup data for billing details when present and needed`() = runTest(dispatcher) {
        val linkAccountWithSignupData = LinkAccount(
            TestFactory.CONSUMER_SESSION,
            nameUsedInSignup = "Signup Name",
            phoneNumberUsedInSignup = "+1555000999"
        )

        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        billingAddress = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.billingAddress?.copy(
                            name = null // No name in billing address, should use signup data
                        )
                    )
                )
            )
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails() // No default name
        )

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val navigationManager = TestNavigationManager()

        val viewModel = createViewModel(
            linkAccount = linkAccountWithSignupData,
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            dismissWithResult = ::dismissWithResult,
            navigationManager = navigationManager
        )

        viewModel.onPrimaryButtonClicked()

        // Should proceed with payment completion using signup data, not navigate to update screen
        val completedResult = linkActivityResult as? LinkActivityResult.Completed
        assertThat(completedResult).isNotNull()

        // Should not navigate to update screen since signup data satisfied the requirements
        val navigatedRoutes = navigationManager.emittedIntents
            .filterIsInstance<NavigationIntent.NavigateTo>()
            .map { it.route }
        assertThat(navigatedRoutes).doesNotContain(LinkScreen.UpdateCard.route)
    }

    @Test
    fun `navigates to update screen when billing details are insufficient`() = runTest(dispatcher) {
        val linkAccountWithIncompleteData = LinkAccount(
            TestFactory.CONSUMER_SESSION,
            phoneNumberUsedInSignup = "+1555000999" // Has phone but no name
        )

        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        billingAddress = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.billingAddress?.copy(
                            name = null // No name in billing address
                        )
                    )
                )
            )
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = CollectionMode.Always,
                phone = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails() // No defaults
        )

        val navigationManager = TestNavigationManager()

        val viewModel = createViewModel(
            linkAccount = linkAccountWithIncompleteData,
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            navigationManager = navigationManager
        )

        viewModel.onPrimaryButtonClicked()

        // Should navigate to update screen since name is required but not available
        // Check that navigation occurred to UpdateCard with the correct payment details ID
        val navigatedRoutes = navigationManager.emittedIntents
            .filterIsInstance<NavigationIntent.NavigateTo>()
            .map { it.route }

        val expectedRoute = LinkScreen.UpdateCard(
            paymentDetailsId = "pm_123", // This is the ID from TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
            billingDetailsUpdateFlow = LinkScreen.UpdateCard.BillingDetailsUpdateFlow(cvc = null)
        )
        assertThat(navigatedRoutes).contains(expectedRoute)
    }

    @Test
    fun `proceeds with confirmation when effective billing details satisfy requirements`() = runTest(dispatcher) {
        val linkAccountWithCompleteData = LinkAccount(
            TestFactory.CONSUMER_SESSION,
            nameUsedInSignup = "Complete Name",
            phoneNumberUsedInSignup = "+1555000999"
        )

        val linkAccountManager = WalletLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        billingAddress = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.billingAddress?.copy(
                            name = null // No name in billing address, but signup data provides it
                        )
                    )
                )
            )
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = CollectionMode.Always,
                phone = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails() // No defaults
        )

        var linkActivityResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            linkActivityResult = result
        }

        val navigationManager = TestNavigationManager()

        val viewModel = createViewModel(
            linkAccount = linkAccountWithCompleteData,
            linkAccountManager = linkAccountManager,
            configuration = configuration,
            dismissWithResult = ::dismissWithResult,
            navigationManager = navigationManager
        )

        viewModel.onPrimaryButtonClicked()

        // Should proceed with confirmation since effective billing details satisfy requirements
        val completedResult = linkActivityResult as? LinkActivityResult.Completed
        assertThat(completedResult).isNotNull()

        // Should not navigate to update screen
        val navigatedRoutes = navigationManager.emittedIntents
            .filterIsInstance<NavigationIntent.NavigateTo>()
            .map { it.route }
        assertThat(navigatedRoutes).doesNotContain(LinkScreen.UpdateCard.route)
    }

    companion object {
        private val CARD_PROCESSING_DELAY = 1.seconds
        private val CARD_PROCESSING_COMPLETION_TIME = 1.1.seconds
    }
}

private open class WalletLinkAccountManager : FakeLinkAccountManager() {
    val listPaymentDetailsCalls = arrayListOf<Set<String>>()
    val updatePaymentDetailsCalls = arrayListOf<ConsumerPaymentDetailsUpdateParams>()
    val deletePaymentDetailsCalls = arrayListOf<String>()

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        listPaymentDetailsCalls.add(paymentMethodTypes)
        return super.listPaymentDetails(paymentMethodTypes)
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        phone: String?
    ): Result<ConsumerPaymentDetails> {
        updatePaymentDetailsCalls.add(updateParams)
        return super.updatePaymentDetails(updateParams, phone)
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit> {
        deletePaymentDetailsCalls.add(paymentDetailsId)
        return super.deletePaymentDetails(paymentDetailsId)
    }
}

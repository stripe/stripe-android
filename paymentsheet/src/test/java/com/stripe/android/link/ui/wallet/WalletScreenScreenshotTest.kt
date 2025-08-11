package com.stripe.android.link.ui.wallet

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.screenshottesting.Orientation
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import org.junit.Rule
import org.junit.Test

internal class WalletScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        Orientation.entries,
        SystemAppearance.entries
    )

    @Test
    fun testEmptyState() {
        snapshot(
            state = walletUiState(
                paymentDetailsList = emptyList(),
                selectedItem = null,
            )
        )
    }

    @Test
    fun testCollapsedState() {
        snapshot(
            state = walletUiState()
        )
    }

    @Test
    fun testCollapsedStateWithUnavailable() {
        snapshot(
            state = walletUiState(
                cardBrandFilter = RejectCardBrands(CardBrand.Visa)
            )
        )
    }

    @Test
    fun testExpandedState() {
        snapshot(
            state = walletUiState(userSetIsExpanded = true),
        )
    }

    @Test
    fun testExpandedStateWithUnavailable() {
        snapshot(
            state = walletUiState(
                userSetIsExpanded = true,
                cardBrandFilter = RejectCardBrands(CardBrand.Visa)
            ),
        )
    }

    @Test
    fun testCollapsedStateWithExpiredCardCollection() {
        snapshot(
            state = walletUiState(
                userSetIsExpanded = false,

            ),
        )
    }

    @Test
    fun testPayButtonCompletedState() {
        snapshot(
            state = walletUiState(userSetIsExpanded = true),
        )
    }

    @Test
    fun testPayButtonProcessingState() {
        snapshot(
            state = walletUiState(
                isProcessing = true,
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testPayButtonDisabledStateDueToExpiredCard() {
        val paymentDetailsList = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD.copy(
                expiryYear = 1999
            ),
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
        snapshot(
            state = walletUiState(
                paymentDetailsList = paymentDetailsList,
                selectedItem = paymentDetailsList.firstOrNull(),
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testPayButtonDisabledStateDueToCvcCheck() {
        val paymentDetailsList = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD.copy(
                cvcCheck = CvcCheck.Fail
            ),
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
        snapshot(
            state = walletUiState(
                paymentDetailsList = paymentDetailsList,
                selectedItem = paymentDetailsList.firstOrNull(),
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testBankAccountSelectedState() {
        snapshot(
            state = walletUiState(
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull {
                    it is ConsumerPaymentDetails.BankAccount
                },
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testAlertMessage() {
        snapshot(
            state = walletUiState(
                alertMessage = "Something went wrong".resolvableString,
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testLongExpandedPaymentDetails() {
        snapshot(
            state = walletUiState(
                paymentDetailsList = (1..100).map { index ->
                    val card = CONSUMER_PAYMENT_DETAILS_CARD
                    CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        id = "${card.id}_$index"
                    )
                },
                userSetIsExpanded = true,
            ),
        )
    }

    @Test
    fun testCannotAddCreditCard() {
        snapshot(
            state = walletUiState(
                addPaymentMethodOptions = emptyList(),
                userSetIsExpanded = true,
            ),
        )
    }

    private fun walletUiState(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> =
            TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
        selectedItem: ConsumerPaymentDetails.PaymentDetails? = paymentDetailsList.firstOrNull(),
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        hasCompleted: Boolean = false,
        isProcessing: Boolean = false,
        expiryDateInput: FormFieldEntry = FormFieldEntry(null),
        cvcInput: FormFieldEntry = FormFieldEntry(null),
        alertMessage: ResolvableString? = null,
        addPaymentMethodOptions: List<AddPaymentMethodOption> = listOf(AddPaymentMethodOption.Card),
        userSetIsExpanded: Boolean = false,
        signupToggleEnabled: Boolean = false,
    ): WalletUiState {
        return WalletUiState(
            paymentDetailsList = paymentDetailsList,
            email = "email@email.com",
            cardBrandFilter = cardBrandFilter,
            selectedItemId = selectedItem?.id,
            isProcessing = isProcessing,
            isSettingUp = false,
            merchantName = "Example Inc.",
            primaryButtonLabel = primaryButtonLabel,
            secondaryButtonLabel = secondaryButtonLabel,
            hasCompleted = hasCompleted,
            addPaymentMethodOptions = addPaymentMethodOptions,
            userSetIsExpanded = userSetIsExpanded,
            expiryDateInput = expiryDateInput,
            cvcInput = cvcInput,
            alertMessage = alertMessage,
            collectMissingBillingDetailsForExistingPaymentMethods = true,
            signupToggleEnabled = signupToggleEnabled
        )
    }

    private fun snapshot(state: WalletUiState) {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                WalletBody(
                    state = state,
                    onItemSelected = {},
                    onExpandedChanged = {},
                    onPrimaryButtonClick = {},
                    onPayAnotherWayClicked = {},
                    onRemoveClicked = {},
                    onUpdateClicked = {},
                    onSetDefaultClicked = {},
                    showBottomSheetContent = {},
                    hideBottomSheetContent = {},
                    onAddPaymentMethodOptionClicked = {},
                    onDismissAlert = {},
                    onLogoutClicked = {},
                    expiryDateController = SimpleTextFieldController(DateConfig()),
                    cvcController = CvcController(
                        cardBrandFlow = stateFlowOf(CardBrand.Unknown)
                    )
                )
            }
        }
    }

    companion object {
        private val primaryButtonLabel = "Pay $50".resolvableString
        private val secondaryButtonLabel = "Pay another way".resolvableString
    }
}

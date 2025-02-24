package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import org.junit.Rule
import org.junit.Test

internal class WalletScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

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
    fun testExpandedState() {
        snapshot(
            state = walletUiState(),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonCompletedState() {
        snapshot(
            state = walletUiState(),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonProcessingState() {
        snapshot(
            state = walletUiState(
                isProcessing = true,
            ),
            isExpanded = true
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
            ),
            isExpanded = true
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
            ),
            isExpanded = true
        )
    }

    @Test
    fun testBankAccountSelectedState() {
        snapshot(
            state = walletUiState(
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull {
                    it is ConsumerPaymentDetails.BankAccount
                },
            ),
            isExpanded = true
        )
    }

    @Test
    fun testAlertMessage() {
        snapshot(
            state = walletUiState(
                alertMessage = "Something went wrong".resolvableString
            ),
            isExpanded = true
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
                }
            ),
            isExpanded = true
        )
    }

    @Test
    fun testCannotAddCreditCard() {
        snapshot(
            state = walletUiState(
                canAddNewPaymentMethod = false,
            ),
            isExpanded = true
        )
    }

    private fun walletUiState(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> =
            TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
        selectedItem: ConsumerPaymentDetails.PaymentDetails? = paymentDetailsList.firstOrNull(),
        hasCompleted: Boolean = false,
        isProcessing: Boolean = false,
        expiryDateInput: FormFieldEntry = FormFieldEntry(null),
        cvcInput: FormFieldEntry = FormFieldEntry(null),
        alertMessage: ResolvableString? = null,
        canAddNewPaymentMethod: Boolean = true,
    ): WalletUiState {
        return WalletUiState(
            paymentDetailsList = paymentDetailsList,
            selectedItem = selectedItem,
            hasCompleted = hasCompleted,
            isProcessing = isProcessing,
            primaryButtonLabel = primaryButtonLabel,
            expiryDateInput = expiryDateInput,
            cvcInput = cvcInput,
            alertMessage = alertMessage,
            canAddNewPaymentMethod = canAddNewPaymentMethod
        )
    }

    private fun snapshot(
        state: WalletUiState,
        isExpanded: Boolean = false
    ) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                WalletBody(
                    state = state,
                    isExpanded = isExpanded,
                    onItemSelected = {},
                    onExpandedChanged = {},
                    onPrimaryButtonClick = {},
                    onPayAnotherWayClicked = {},
                    onRemoveClicked = {},
                    onSetDefaultClicked = {},
                    showBottomSheetContent = {},
                    hideBottomSheetContent = {},
                    onAddNewPaymentMethodClicked = {},
                    onDismissAlert = {},
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
    }
}

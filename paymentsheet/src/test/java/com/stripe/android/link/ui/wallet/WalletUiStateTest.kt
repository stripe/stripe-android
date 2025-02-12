package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.Test

class WalletUiStateTest {

    @Test
    fun testCompletedButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            hasCompleted = true,
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)
    }

    @Test
    fun testProcessingButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            isProcessing = true
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun testDisabledButtonStateForExpiredCard() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    expiryYear = 1900
                ),
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testDisabledButtonStateForCvcRecollection() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    cvcCheck = CvcCheck.Unchecked
                )
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099),
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testShowBankAccountTermsForSelectedBankPaymentMethod() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
        )

        assertThat(state.showBankAccountTerms).isTrue()
    }

    @Test
    fun testNoBankAccountTermsForSelectedNonBankPaymentMethod() {
        val state = walletUiState()

        assertThat(state.showBankAccountTerms).isFalse()
    }

    @Test
    fun testDisabledButtonStateForExpiredCardWithIncompleteExpiryDate() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900),
            expiryDateInput = FormFieldEntry("", isComplete = false)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForExpiredCardWithCompleteExpiryDateAndIncompleteCvc() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900),
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForExpiredCardWithCompleteExpiryDate() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900),
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testDisabledButtonStateForCardRequiringCvcWithIncompleteCvc() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                cvcCheck = CvcCheck.Unchecked
            ),
            cvcInput = FormFieldEntry("12", isComplete = false)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForCardRequiringCvcWithCompleteCvc() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                cvcCheck = CvcCheck.Unchecked
            ),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testEnabledButtonStateForValidCardWithBothInputsComplete() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099),
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    private fun walletUiState(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> =
            TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
        selectedItem: ConsumerPaymentDetails.PaymentDetails? = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
        hasCompleted: Boolean = false,
        isProcessing: Boolean = false,
        primaryButtonLabel: ResolvableString = LINK_WALLET_PRIMARY_BUTTON_LABEL,
        expiryDateInput: FormFieldEntry = FormFieldEntry(null),
        cvcInput: FormFieldEntry = FormFieldEntry(null),
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
            canAddNewPaymentMethod = canAddNewPaymentMethod,
        )
    }
}

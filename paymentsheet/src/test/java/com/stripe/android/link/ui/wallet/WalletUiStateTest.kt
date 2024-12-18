package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CvcCheck
import org.junit.Test

class WalletUiStateTest {

    @Test
    fun testCompletedButtonState() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            hasCompleted = true,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)
    }

    @Test
    fun testProcessingButtonState() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            hasCompleted = false,
            isProcessing = true,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun testDisabledButtonStateForExpiredCard() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    expiryYear = 1900
                ),
            hasCompleted = false,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testDisabledButtonStateForCvcRecollection() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    cvcCheck = CvcCheck.Unchecked
                ),
            hasCompleted = false,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonState() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(expiryYear = 2099),
            hasCompleted = false,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testShowBankAccountTermsForSelectedBankPaymentMethod() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            hasCompleted = false,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.showBankAccountTerms).isTrue()
    }

    @Test
    fun testNoBankAccountTermsForSelectedNonBankPaymentMethod() {
        val state = WalletUiState(
            paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            hasCompleted = false,
            isProcessing = false,
            primaryButtonLabel = LINK_WALLET_PRIMARY_BUTTON_LABEL
        )

        assertThat(state.showBankAccountTerms).isFalse()
    }
}

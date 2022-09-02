package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test

class WalletUiStateTest {

    @Test
    fun `Primary button is enabled correctly`() {
        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = mockPaymentDetailsList(),
            selectedItem = mockPaymentDetailsList().last(),
            isProcessing = false,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun `Primary button state is correct when the payment has completed`() {
        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = mockPaymentDetailsList(),
            selectedItem = mockPaymentDetailsList().last(),
            isProcessing = false,
            hasCompleted = true
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)
    }

    @Test
    fun `Primary button state is correct when the payment is processing`() {
        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = mockPaymentDetailsList(),
            selectedItem = mockPaymentDetailsList().last(),
            isProcessing = true,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun `Primary button is disabled if selected card is expired and form hasn't been filled out`() {
        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = mockPaymentDetailsList(),
            selectedItem = mockPaymentDetailsList().first(),
            isProcessing = false,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun `Primary button is enabled if selected card is expired, but the form has been filled out`() {
        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = mockPaymentDetailsList(),
            selectedItem = mockPaymentDetailsList().first(),
            isProcessing = false,
            hasCompleted = false,
            expiryDateInput = FormFieldEntry("1226", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    private fun mockPaymentDetailsList() = listOf(
        ConsumerPaymentDetails.Card(
            "id1",
            true,
            2022,
            1,
            CardBrand.Visa,
            "4242",
            CvcCheck.Pass
        ),
        ConsumerPaymentDetails.Card(
            "id2",
            false,
            2026,
            12,
            CardBrand.MasterCard,
            "4444",
            CvcCheck.Fail
        )
    )
}

package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test
import java.util.Calendar
import kotlin.random.Random

class WalletUiStateTest {

    @Test
    fun `Primary button is enabled correctly`() {
        val validCard = mockCard()

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = false,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun `Primary button state is correct when the payment has completed`() {
        val validCard = mockCard()

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = false,
            hasCompleted = true
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)
    }

    @Test
    fun `Primary button state is correct when the payment is processing`() {
        val validCard = mockCard()

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = true,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun `Primary button is disabled if selected card is expired and form hasn't been filled out`() {
        val validCard = mockCard(isExpired = true)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = false,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun `Primary button is enabled if selected card is expired, but the form has been filled out`() {
        val validCard = mockCard(isExpired = true)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = false,
            hasCompleted = false,
            expiryDateInput = FormFieldEntry("1226", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    private fun mockCard(
        isExpired: Boolean = false,
        cvcCheck: CvcCheck = CvcCheck.Pass
    ): ConsumerPaymentDetails.Card {
        val id = Random.nextInt()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val expiryYear = if (isExpired) year - 1 else year + 1

        return ConsumerPaymentDetails.Card(
            id = "id$id",
            isDefault = true,
            expiryYear = expiryYear,
            expiryMonth = 1,
            brand = CardBrand.Visa,
            last4 = "4242",
            cvcCheck = cvcCheck
        )
    }
}

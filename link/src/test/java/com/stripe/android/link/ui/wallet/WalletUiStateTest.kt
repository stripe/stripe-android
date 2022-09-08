package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.ErrorMessage
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
    fun `Encountering an error stops processing`() {
        val validCard = mockCard()

        val initialState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(validCard),
            selectedItem = validCard,
            isProcessing = false,
            hasCompleted = false
        )

        val processingState = initialState.setProcessing()

        val errorMessage = ErrorMessage.Raw("Something went wrong")
        val finalState = processingState.updateWithError(errorMessage)

        assertThat(finalState.errorMessage).isEqualTo(errorMessage)
        assertThat(finalState.isProcessing).isFalse()
    }

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
    fun `Primary button is disabled if selected payment details aren't valid`() {
        val bankAccount = mockBankAccount()

        val uiState = WalletUiState(
            supportedTypes = setOf(SupportedPaymentMethod.Card.type),
            paymentDetailsList = listOf(bankAccount),
            selectedItem = bankAccount,
            isProcessing = false,
            hasCompleted = false,
            expiryDateInput = FormFieldEntry(value = null),
            cvcInput = FormFieldEntry(value = null)
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
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
        val expiredCard = mockCard(isExpired = true)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(expiredCard),
            selectedItem = expiredCard,
            isProcessing = false,
            hasCompleted = false
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun `Primary button is enabled if selected card is expired, but the form has been filled out`() {
        val expiredCard = mockCard(isExpired = true)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(expiredCard),
            selectedItem = expiredCard,
            isProcessing = false,
            hasCompleted = false,
            expiryDateInput = FormFieldEntry("1226", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun `Primary button is disabled if required CVC check hasn't been filled out`() {
        val uncheckedCard = mockCard(cvcCheck = CvcCheck.Fail)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(uncheckedCard),
            selectedItem = uncheckedCard,
            isProcessing = false,
            hasCompleted = false,
            cvcInput = FormFieldEntry(value = null)
        )

        assertThat(uiState.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun `Primary button is enabled if required CVC check has been filled out`() {
        val uncheckedCard = mockCard(cvcCheck = CvcCheck.Fail)

        val uiState = WalletUiState(
            supportedTypes = SupportedPaymentMethod.allTypes,
            paymentDetailsList = listOf(uncheckedCard),
            selectedItem = uncheckedCard,
            isProcessing = false,
            hasCompleted = false,
            cvcInput = FormFieldEntry(value = "123", isComplete = true)
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

    private fun mockBankAccount(): ConsumerPaymentDetails.BankAccount {
        val id = Random.nextInt()
        return ConsumerPaymentDetails.BankAccount(
            id = "id$id",
            isDefault = false,
            bankName = "Stripe Test Bank",
            bankIconCode = null,
            last4 = "4242"
        )
    }
}

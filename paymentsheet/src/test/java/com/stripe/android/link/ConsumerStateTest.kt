package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import org.junit.Test

class ConsumerStateTest {

    @Test
    fun `withPaymentDetailsResponse with empty existing state creates new payment details`() {
        val emptyState = ConsumerState(emptyList())
        val response = ConsumerPaymentDetails(
            paymentDetails = listOf(CONSUMER_PAYMENT_DETAILS_CARD, CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        )

        val result = emptyState.withPaymentDetailsResponse(response)

        assertThat(result.paymentDetails).hasSize(2)
        assertThat(result.paymentDetails[0].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_CARD)
        assertThat(result.paymentDetails[0].collectedCvc).isNull()
        assertThat(result.paymentDetails[0].billingPhone).isNull()
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
        assertThat(result.paymentDetails[1].billingPhone).isNull()
    }

    @Test
    fun `withPaymentDetailsResponse preserves existing local data for matching payment details`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = "+0987654321"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "1234")
        val updatedBankAccount = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT.copy(last4 = "5678")
        val response = ConsumerPaymentDetails(
            paymentDetails = listOf(updatedCard, updatedBankAccount)
        )

        val result = existingState.withPaymentDetailsResponse(response)

        assertThat(result.paymentDetails).hasSize(2)
        // Check that backend data was updated but local data was preserved
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890")
        assertThat(result.paymentDetails[1].details).isEqualTo(updatedBankAccount)
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
        assertThat(result.paymentDetails[1].billingPhone).isEqualTo("+0987654321")
    }

    @Test
    fun `withPaymentDetailsResponse handles mixed existing and new payment details`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "1234")
        val response = ConsumerPaymentDetails(
            paymentDetails = listOf(
                updatedCard,
                CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            )
        )

        val result = existingState.withPaymentDetailsResponse(response)

        assertThat(result.paymentDetails).hasSize(3)
        // Existing payment detail should preserve local data
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890")
        // New payment details should have null local data
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
        assertThat(result.paymentDetails[1].billingPhone).isNull()
    }

    @Test
    fun `withPaymentDetailsResponse with no matching IDs creates all new payment details`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val newCard = ConsumerPaymentDetails.Card(
            id = "pm_different",
            last4 = "5678",
            expiryYear = 2025,
            expiryMonth = 6,
            brand = CardBrand.MasterCard,
            cvcCheck = CvcCheck.Pass,
            isDefault = false,
            networks = emptyList(),
            funding = "DEBIT",
            nickname = "New Card",
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = "Jane Doe",
                line1 = "456 Oak St",
                line2 = null,
                locality = "New City",
                administrativeArea = "NY",
                countryCode = CountryCode.US,
                postalCode = "54321"
            )
        )
        val response = ConsumerPaymentDetails(paymentDetails = listOf(newCard))

        val result = existingState.withPaymentDetailsResponse(response)

        assertThat(result.paymentDetails).hasSize(1)
        assertThat(result.paymentDetails[0].details).isEqualTo(newCard)
        assertThat(result.paymentDetails[0].collectedCvc).isNull()
        assertThat(result.paymentDetails[0].billingPhone).isNull()
    }

    @Test
    fun `withPaymentDetailsResponse with empty response returns empty state`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)
        val response = ConsumerPaymentDetails(paymentDetails = emptyList())

        val result = existingState.withPaymentDetailsResponse(response)

        assertThat(result.paymentDetails).isEmpty()
    }

    @Test
    fun `withPaymentDetailsResponse removes cached payment details not in response`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = "+0987654321"
            ),
        )
        val existingState = ConsumerState(existingPaymentDetails)

        // Response only includes the card, not the bank account or passthrough
        val response = ConsumerPaymentDetails(
            paymentDetails = listOf(CONSUMER_PAYMENT_DETAILS_CARD)
        )

        val result = existingState.withPaymentDetailsResponse(response)

        // Only the card should remain, with preserved local data
        assertThat(result.paymentDetails).hasSize(1)
        assertThat(result.paymentDetails[0].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_CARD)
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890")
    }

    @Test
    fun `fromResponse creates new state with null local fields`() {
        val response = ConsumerPaymentDetails(
            paymentDetails = listOf(
                CONSUMER_PAYMENT_DETAILS_CARD,
                CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            )
        )

        val result = ConsumerState.fromResponse(response)

        assertThat(result.paymentDetails).hasSize(3)
        assertThat(result.paymentDetails[0].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_CARD)
        assertThat(result.paymentDetails[0].collectedCvc).isNull()
        assertThat(result.paymentDetails[0].billingPhone).isNull()
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
        assertThat(result.paymentDetails[1].billingPhone).isNull()
    }

    @Test
    fun `fromResponse with empty response creates empty state`() {
        val response = ConsumerPaymentDetails(paymentDetails = emptyList())

        val result = ConsumerState.fromResponse(response)

        assertThat(result.paymentDetails).isEmpty()
    }

    @Test
    fun `withUpdatedPaymentDetail updates matching payment detail and preserves local data`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = "+0987654321"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "9999")
        val newPhone = "+1111111111"

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = updatedCard,
            billingPhone = newPhone
        )

        assertThat(result.paymentDetails).hasSize(2)
        // Check that the matching payment detail was updated
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo(newPhone)
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123") // Preserved
        // Check that the other payment detail keeps its existing phone (not overwritten)
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].billingPhone).isEqualTo("+0987654321") // Existing phone preserved
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
    }

    @Test
    fun `withUpdatedPaymentDetail with no matching ID keeps existing payment details unchanged`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = null // No phone
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val differentCard = ConsumerPaymentDetails.Card(
            id = "pm_different",
            last4 = "1111",
            expiryYear = 2025,
            expiryMonth = 6,
            brand = CardBrand.MasterCard,
            cvcCheck = CvcCheck.Pass,
            isDefault = false,
            networks = emptyList(),
            funding = "DEBIT",
            nickname = "Different Card",
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = "Jane Doe",
                line1 = "456 Oak St",
                line2 = null,
                locality = "New City",
                administrativeArea = "NY",
                countryCode = CountryCode.US,
                postalCode = "54321"
            )
        )

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = differentCard,
            billingPhone = "+9999999999"
        )

        assertThat(result.paymentDetails).hasSize(2)
        // First payment detail keeps existing phone (has one already)
        assertThat(result.paymentDetails[0].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_CARD)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890") // Unchanged
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        // Second payment detail gets the new phone (didn't have one)
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].billingPhone).isEqualTo("+9999999999") // Applied
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
    }

    @Test
    fun `withUpdatedPaymentDetail with null billingPhone preserves existing billingPhone`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "9999")

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = updatedCard,
            billingPhone = null // null means preserve existing
        )

        assertThat(result.paymentDetails).hasSize(1)
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890") // Preserved existing
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123") // Preserved
    }

    @Test
    fun `withUpdatedPaymentDetail with explicit billingPhone updates it`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "9999")
        val newPhone = "+9999999999"

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = updatedCard,
            billingPhone = newPhone
        )

        assertThat(result.paymentDetails).hasSize(1)
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo(newPhone) // Updated
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123") // Preserved
    }

    @Test
    fun `withUpdatedPaymentDetail applies billing phone to other payment details without phones`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = null // No phone
            ),
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "9999")
        val newPhone = "+1111111111"

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = updatedCard,
            billingPhone = newPhone
        )

        assertThat(result.paymentDetails).hasSize(3)
        // Updated payment detail
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo(newPhone)
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        // Bank account should get the new phone (was null)
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].billingPhone).isEqualTo(newPhone) // Applied
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
    }

    @Test
    fun `withUpdatedPaymentDetail with null billing phone does not propagate to other payment details`() {
        val existingPaymentDetails = listOf(
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = "123",
                billingPhone = "+1234567890"
            ),
            LinkPaymentMethod(
                details = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                collectedCvc = null,
                billingPhone = null // No phone
            )
        )
        val existingState = ConsumerState(existingPaymentDetails)

        val updatedCard = CONSUMER_PAYMENT_DETAILS_CARD.copy(last4 = "9999")

        val result = existingState.withUpdatedPaymentDetail(
            updatedPayment = updatedCard,
            billingPhone = null // No phone to propagate
        )

        assertThat(result.paymentDetails).hasSize(2)
        // Updated payment detail preserves existing phone
        assertThat(result.paymentDetails[0].details).isEqualTo(updatedCard)
        assertThat(result.paymentDetails[0].billingPhone).isEqualTo("+1234567890") // Preserved
        assertThat(result.paymentDetails[0].collectedCvc).isEqualTo("123")
        // Other payment detail remains unchanged (still null phone)
        assertThat(result.paymentDetails[1].details).isEqualTo(CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
        assertThat(result.paymentDetails[1].billingPhone).isNull() // Still null
        assertThat(result.paymentDetails[1].collectedCvc).isNull()
    }
}

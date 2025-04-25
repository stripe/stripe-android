package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.BILLING_DETAILS_FORM_DETAILS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.billingDetailsFormState
import org.junit.Test

internal class CardDetailsEntryTest {

    @Test
    fun `isComplete() should be true when expiry is editable and both expiry fields are present`() {
        val entry = createEntry(expMonth = 12, expYear = 2030, expiryDateEditable = true)
        assertThat(entry.isComplete()).isTrue()
    }

    @Test
    fun `isComplete() should be false when expiry is editable and expiry month is null`() {
        val entry = createEntry(expMonth = null, expYear = 2030, expiryDateEditable = true)
        assertThat(entry.isComplete()).isFalse()
    }

    @Test
    fun `isComplete() should be false when expiry is editable and expiry year is null`() {
        val entry = createEntry(expMonth = 12, expYear = null, expiryDateEditable = true)
        assertThat(entry.isComplete()).isFalse()
    }

    @Test
    fun `isComplete() should be false when expiry is editable and both expiry fields are null`() {
        val entry = createEntry(expMonth = null, expYear = null, expiryDateEditable = true)
        assertThat(entry.isComplete()).isFalse()
    }

    @Test
    fun `isComplete() should be true when expiry is not editable regardless of expiry values`() {
        assertThat(createEntry(expMonth = 12, expYear = 2030, expiryDateEditable = false).isComplete()).isTrue()
        assertThat(createEntry(expMonth = null, expYear = 2030, expiryDateEditable = false).isComplete()).isTrue()
        assertThat(createEntry(expMonth = 12, expYear = null, expiryDateEditable = false).isComplete()).isTrue()
        assertThat(createEntry(expMonth = null, expYear = null, expiryDateEditable = false).isComplete()).isTrue()
    }

//    @Test
//    fun `isComplete() should be true when billing details is valid`() {
//        val entry = createEntry(
//            billingDetailsFormState = billingDetailsFormState()
//        )
//        assertThat(entry.isComplete()).isTrue()
//    }

//    @Test
//    fun `isComplete() should be false when billing address is invalid`() {
//        val entry = createEntry(
//            expMonth = 12,
//            expYear = 2030,
//            billingDetailsFormState = billingDetailsFormState(
//                line1 = FormFieldEntry("", isComplete = false),
//            ),
//            addressCollectionMode = AddressCollectionMode.Full
//        )
//        assertThat(entry.isComplete()).isFalse()
//    }

    @Test
    fun `hasChanged() should return true when card brand choice changes`() {
        val originalCardBrandChoice = CardBrandChoice(CardBrand.Visa, true)
        val newCardBrandChoice = CardBrandChoice(CardBrand.MasterCard, true)

        val entry = createEntry(
            cardBrandChoice = newCardBrandChoice,
            expMonth = 12,
            expYear = 2030
        )

        val card = createCard(expiryMonth = 12, expiryYear = 2030)

        assertThat(entry.hasChanged(card, originalCardBrandChoice)).isTrue()
    }

    @Test
    fun `hasChanged() should return true when expiry month changes`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)

        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = 11,
            expYear = 2030
        )

        val card = createCard(expiryMonth = 12, expiryYear = 2030)

        assertThat(entry.hasChanged(card, cardBrandChoice)).isTrue()
    }

    @Test
    fun `hasChanged() should return true when expiry year changes`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)

        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = 12,
            expYear = 2029
        )

        val card = createCard(expiryMonth = 12, expiryYear = 2030)

        assertThat(entry.hasChanged(card, cardBrandChoice)).isTrue()
    }

    @Test
    fun `hasChanged() should return true when both expiry month and year change`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)

        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = 11,
            expYear = 2029
        )

        val card = createCard(expiryMonth = 12, expiryYear = 2030)

        assertThat(entry.hasChanged(card, cardBrandChoice)).isTrue()
    }

//    @Test
//    fun `hasChanged() should return true when billing details changes`() {
//        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)
//
//        val entry = createEntry(
//            billingDetailsFormState = billingDetailsFormState(
//                postalCode = FormFieldEntry("94107", isComplete = true)
//            )
//        )
//
//        val card = createCard()
//
//        assertThat(entry.hasChanged(card, cardBrandChoice)).isTrue()
//    }

    @Test
    fun `hasChanged() should return true when card brand and expiry fields all change`() {
        val originalCardBrandChoice = CardBrandChoice(CardBrand.Visa, true)
        val newCardBrandChoice = CardBrandChoice(CardBrand.MasterCard, true)

        val entry = createEntry(
            cardBrandChoice = newCardBrandChoice,
            expMonth = 11,
            expYear = 2029
        )

        val card = createCard(expiryMonth = 12, expiryYear = 2030)

        assertThat(entry.hasChanged(card, originalCardBrandChoice)).isTrue()
    }

    @Test
    fun `hasChanged() should return false when nothing changes`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)

        val entry = createEntry()

        val card = createCard()

        assertThat(entry.hasChanged(card, cardBrandChoice)).isFalse()
    }

    @Test
    fun `toUpdateParams() should correctly convert to CardUpdateParams`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)
        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = 12,
            expYear = 2030
        )

        val params = entry.toUpdateParams(billingDetailsEntry())

        assertThat(params.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(params.expiryMonth).isEqualTo(12)
        assertThat(params.expiryYear).isEqualTo(2030)
        assertThat(params.billingDetails).isEqualTo(BILLING_DETAILS_FORM_DETAILS)
    }

    @Test
    fun `toUpdateParams() should handle null expiry values`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)
        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = null,
            expYear = null
        )

        val params = entry.toUpdateParams(billingDetailsEntry())

        assertThat(params.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(params.expiryMonth).isNull()
        assertThat(params.expiryYear).isNull()
        assertThat(params.billingDetails).isEqualTo(BILLING_DETAILS_FORM_DETAILS)
    }

    @Test
    fun `toUpdateParams() should include billing details when available`() {
        val cardBrandChoice = CardBrandChoice(CardBrand.Visa, true)

        val entry = createEntry(
            cardBrandChoice = cardBrandChoice,
            expMonth = 12,
            expYear = 2030,
        )

        val params = entry.toUpdateParams(billingDetailsEntry())

        assertThat(params.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(params.expiryMonth).isEqualTo(12)
        assertThat(params.expiryYear).isEqualTo(2030)
        assertThat(params.billingDetails).isEqualTo(BILLING_DETAILS_FORM_DETAILS)
    }

    private fun createEntry(
        cardBrandChoice: CardBrandChoice = CardBrandChoice(CardBrand.Visa, true),
        expMonth: Int? = 12,
        expYear: Int? = 2030,
        expiryDateEditable: Boolean = true,
    ) = CardDetailsEntry(
        cardBrandChoice = cardBrandChoice,
        expiryDateState = ExpiryDateState.create(
            editPayload = createCard(expiryMonth = expMonth, expiryYear = expYear),
            enabled = expiryDateEditable
        ),
    )

    private fun billingDetailsEntry(
        billingDetailsFormState: BillingDetailsFormState = billingDetailsFormState()
    ): BillingDetailsEntry {
        return BillingDetailsEntry(billingDetailsFormState)
    }

    private fun createCard(
        expiryMonth: Int? = 12,
        expiryYear: Int? = 2030
    ): EditCardPayload {
        val card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
            brand = CardBrand.Visa,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            last4 = "4242"
        )

        return EditCardPayload.create(card, null)
    }
}

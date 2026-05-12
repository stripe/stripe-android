package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.LINK_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.US_BANK_ACCOUNT
import com.stripe.android.paymentsheet.R
import org.junit.Test
import com.stripe.android.R as StripeR

class SavedPaymentMethodIconTest {
    @Test
    fun `on card payment method, should return proper card brand icon`() {
        assertThat(
            CARD_PAYMENT_METHOD.getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)

        assertThat(
            CARD_PAYMENT_METHOD.copy(
                card = CARD_PAYMENT_METHOD.card?.copy(brand = CardBrand.MasterCard)
            ).getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_mastercard_ref)
    }

    @Test
    fun `on SEPA payment method, should return proper SEPA icon`() {
        assertThat(
            SEPA_DEBIT_PAYMENT_METHOD.getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_sepa_ref)
    }

    @Test
    fun `on US bank account payment method, should return proper US bank account icon`() {
        assertThat(
            US_BANK_ACCOUNT.getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(StripeR.drawable.stripe_ic_bank_stripe)
    }

    @Test
    fun `on display brand available for card payment method, should return proper brand icon`() {
        assertThat(
            CARD_PAYMENT_METHOD.copy(
                card = CARD_PAYMENT_METHOD.card?.copy(
                    displayBrand = "cartes_bancaires"
                )
            ).getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires_ref)
    }

    @Test
    fun `on display brand available for card payment method, a null value defaults back to visa`() {
        assertThat(
            CARD_PAYMENT_METHOD.copy(
                card = CARD_PAYMENT_METHOD.card?.copy()
            ).getSavedPaymentMethodIcon(linkBrand = LinkBrand.Link)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
    }

    @Test
    fun `on Link payment method with Link brand, should return Link day icon`() {
        assertThat(
            LINK_PAYMENT_METHOD.getSavedPaymentMethodIcon(
                linkBrand = LinkBrand.Link,
                showNightIcon = false,
            )
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_day)
    }

    @Test
    fun `on Link payment method with Notlink brand, should return arrow-only icon`() {
        assertThat(
            LINK_PAYMENT_METHOD.getSavedPaymentMethodIcon(linkBrand = LinkBrand.Onelink)
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_arrow)
    }

    @Test
    fun `on Link payment method with forVerticalMode, should return arrow-only icon`() {
        assertThat(
            LINK_PAYMENT_METHOD.getSavedPaymentMethodIcon(
                linkBrand = LinkBrand.Link,
                forVerticalMode = true,
            )
        ).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_arrow)
    }
}

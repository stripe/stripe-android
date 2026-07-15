package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class ExpressButtonTest {

    @Test
    fun `Link create uses link brand from payment method metadata`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkBrand = LinkBrand.Onelink,
        )

        val button = ExpressButton.Link.create(paymentMethodMetadata)

        assertThat(button.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `GooglePay create uses billing details collection configuration from payment method metadata`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )

        val button = ExpressButton.GooglePay.create(paymentMethodMetadata)

        assertThat(button.billingAddressParameters)
            .isEqualTo(billingDetailsCollectionConfiguration.toBillingAddressParameters())
    }

    @Test
    fun `GooglePay create uses card brand filter from payment method metadata`() {
        val cardBrandFilter = PaymentSheetCardBrandFilter(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.Allowed(
                brands = listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa),
            ),
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cardBrandFilter = cardBrandFilter,
        )

        val button = ExpressButton.GooglePay.create(paymentMethodMetadata)

        assertThat(button.cardBrandFilter).isSameInstanceAs(cardBrandFilter)
    }

    @Test
    fun `GooglePay create uses card funding filter from payment method metadata`() {
        val cardFundingFilter = PaymentSheetCardFundingFilter(
            allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Debit),
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cardFundingFilter = cardFundingFilter,
        )

        val button = ExpressButton.GooglePay.create(paymentMethodMetadata)

        assertThat(button.cardFundingFilter).isSameInstanceAs(cardFundingFilter)
    }
}

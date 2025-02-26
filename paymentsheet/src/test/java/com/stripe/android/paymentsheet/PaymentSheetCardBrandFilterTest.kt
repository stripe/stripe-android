package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.toBrandCategory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.junit.Test

class PaymentSheetCardBrandFilterTest {

    @Test
    fun testIsAccepted_allBrandsAccepted() {
        val filter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.All)

        for (brand in CardBrand.entries) {
            assertThat(filter.isAccepted(brand)).isTrue()
        }
    }

    @Test
    fun testIsAccepted_allowedBrands() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa,
            PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        for (brand in CardBrand.entries) {
            val brandCategory = brand.toBrandCategory()
            val isExpectedToBeAccepted =
                brandCategory != null && allowedBrands.contains(brandCategory)
            val isAccepted = filter.isAccepted(brand)
            assertThat(isAccepted)
                .isEqualTo(isExpectedToBeAccepted)
        }
    }

    @Test
    fun testIsAccepted_disallowedBrands() {
        val disallowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Amex,
            PaymentSheet.CardBrandAcceptance.BrandCategory.Discover
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Disallowed(disallowedBrands)
        )

        for (brand in CardBrand.entries) {
            val brandCategory = brand.toBrandCategory()
            val isExpectedToBeAccepted =
                brandCategory == null || !disallowedBrands.contains(brandCategory)
            val isAccepted = filter.isAccepted(brand)
            assertThat(isAccepted)
                .isEqualTo(isExpectedToBeAccepted)
        }
    }

    @Test
    fun testIsAccepted_unknownBrandCategory() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        for (brand in CardBrand.entries) {
            if (brand.toBrandCategory() == null) {
                assertThat(filter.isAccepted(brand)).isFalse()
            }
        }
    }

    @Test
    fun testIsAccepted_allowsBrandWithNullCategory_whenDisallowed() {
        val disallowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Disallowed(disallowedBrands)
        )

        for (brand in CardBrand.entries) {
            if (brand.toBrandCategory() == null) {
                assertThat(filter.isAccepted(brand)).isTrue()
            }
        }
    }

    @Test
    fun `isAccepted(paymentMethod) returns true for non-card payment methods`() {
        val filter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.All)

        val nonCardPaymentMethod = PaymentMethodFactory.usBankAccount()

        assertThat(filter.isAccepted(nonCardPaymentMethod)).isTrue()
    }

    @Test
    fun `isAccepted(paymentMethod) accepts card with allowed brand`() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        val visaCard = PaymentMethodFactory.card(id = "pm_visa").update(
            last4 = "1001",
            addCbcNetworks = false,
            brand = CardBrand.Visa
        )

        assertThat(filter.isAccepted(visaCard)).isTrue()
    }

    @Test
    fun `isAccepted(paymentMethod) rejects card with disallowed brand`() {
        val disallowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Disallowed(disallowedBrands)
        )

        val amexCard = PaymentMethodFactory.card(id = "pm_amex").update(
            last4 = "1001",
            addCbcNetworks = false,
            brand = CardBrand.AmericanExpress
        )

        assertThat(filter.isAccepted(amexCard)).isFalse()
    }

    @Test
    fun `isAccepted(paymentMethod) handles unknown brand gracefully`() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        val unknownBrandCard = PaymentMethodFactory.card(id = "pm_unknown").update(
            last4 = "1001",
            addCbcNetworks = false,
            brand = CardBrand.Unknown
        )

        assertThat(filter.isAccepted(unknownBrandCard)).isFalse()
    }

    @Test
    fun `isAccepted(paymentMethod) uses displayBrand when available`() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        val cardWithDisplayBrand = PaymentMethodFactory.card(id = "pm_mastercard").copy(
            card = PaymentMethod.Card(
                brand = CardBrand.CartesBancaires,
                displayBrand = "mastercard"
            )
        )

        assertThat(filter.isAccepted(cardWithDisplayBrand)).isTrue()
    }

    @Test
    fun `isAccepted(paymentMethod) falls back to brand when displayBrand is null`() {
        val allowedBrands = listOf(
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        )
        val filter = PaymentSheetCardBrandFilter(
            PaymentSheet.CardBrandAcceptance.Allowed(allowedBrands)
        )

        val cardWithoutDisplayBrand = PaymentMethodFactory.card(id = "pm_visa").copy(
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                displayBrand = null
            )
        )

        assertThat(filter.isAccepted(cardWithoutDisplayBrand)).isTrue()
    }

    @Test
    fun `isAccepted(paymentMethod) defaults to Unknown when both brand and displayBrand are null`() {
        val filter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.All)

        val cardWithNoBrand = PaymentMethodFactory.card(id = "pm_no_brand").copy(
            card = PaymentMethod.Card(
                brand = CardBrand.Unknown,
                displayBrand = null
            )
        )

        assertThat(filter.isAccepted(cardWithNoBrand)).isTrue()
    }
}

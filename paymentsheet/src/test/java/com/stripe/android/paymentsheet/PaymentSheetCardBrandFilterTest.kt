package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.toBrandCategory
import com.stripe.android.model.CardBrand
import org.junit.Test

@OptIn(ExperimentalCardBrandFilteringApi::class)
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
}

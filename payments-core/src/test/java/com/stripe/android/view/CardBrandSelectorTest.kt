package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand.CartesBancaires
import com.stripe.android.model.CardBrand.MasterCard
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.CardBrand.Visa
import org.junit.Test

class CardBrandSelectorTest {

    @Test
    fun `Shows unknown brand if the user has made no selection and there is no default`() {
        val cardBrand = selectCardBrandToDisplay(
            userSelectedBrand = null,
            possibleBrands = listOf(Unknown, CartesBancaires, Visa),
            merchantPreferredBrands = emptyList(),
        )

        assertThat(cardBrand).isEqualTo(Unknown)
    }

    @Test
    fun `Shows first matching merchant-preferred brand if the user has made no selection`() {
        val cardBrand = selectCardBrandToDisplay(
            userSelectedBrand = null,
            possibleBrands = listOf(Unknown, CartesBancaires, Visa),
            merchantPreferredBrands = listOf(Visa),
        )

        assertThat(cardBrand).isEqualTo(Visa)
    }

    @Test
    fun `Shows unknown brand if the user has made no selection and there is no valid default`() {
        val cardBrand = selectCardBrandToDisplay(
            userSelectedBrand = null,
            possibleBrands = listOf(Unknown, CartesBancaires, Visa),
            merchantPreferredBrands = listOf(MasterCard),
        )

        assertThat(cardBrand).isEqualTo(Unknown)
    }

    @Test
    fun `Shows user-selected brand if there are possible brands and there is no default`() {
        val cardBrand = selectCardBrandToDisplay(
            userSelectedBrand = CartesBancaires,
            possibleBrands = listOf(Unknown, CartesBancaires, Visa),
            merchantPreferredBrands = emptyList(),
        )

        assertThat(cardBrand).isEqualTo(CartesBancaires)
    }

    @Test
    fun `Shows user-selected brand over merchant-preferred brand`() {
        val cardBrand = selectCardBrandToDisplay(
            userSelectedBrand = CartesBancaires,
            possibleBrands = listOf(Unknown, CartesBancaires, Visa),
            merchantPreferredBrands = listOf(Visa),
        )

        assertThat(cardBrand).isEqualTo(CartesBancaires)
    }
}

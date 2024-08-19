package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import org.junit.Test

class CvcStateTest {

    @Test
    fun `cvc state has correct label for non amex card`() {
        nonAmexCards().forEach { brand ->
            val state = CvcState(
                cardBrand = brand,
                cvc = ""
            )
            assertThat(state.label).isEqualTo(R.string.stripe_cvc_number_hint)
        }
    }

    @Test
    fun `cvc state has correct label for amex card`() {
        val state = CvcState(
            cardBrand = CardBrand.AmericanExpress,
            cvc = ""
        )
        assertThat(state.label).isEqualTo(R.string.stripe_cvc_amex_hint)
    }

    @Test
    fun `cvc should be invalid for cvc lower the cvc count - amex`() {
        val state = CvcState(
            cardBrand = CardBrand.AmericanExpress,
            cvc = ""
        )

        val actual = state.updateCvc("555")
        assertThat(actual).isEqualTo(
            CvcState(
                cardBrand = CardBrand.AmericanExpress,
                cvc = "555"
            )
        )
        assertThat(actual.isValid).isEqualTo(false)
    }

    @Test
    fun `cvc should be valid for cvc equals to cvc count - amex`() {
        val state = CvcState(
            cardBrand = CardBrand.AmericanExpress,
            cvc = ""
        )

        val actual = state.updateCvc("5555")
        assertThat(actual).isEqualTo(
            CvcState(
                cardBrand = CardBrand.AmericanExpress,
                cvc = "5555"
            )
        )
        assertThat(actual.isValid).isEqualTo(true)
    }

    @Test
    fun `cvc should be invalid for cvc lower the cvc count - non-amex`() {
        nonAmexCards().forEach { brand ->
            val state = CvcState(
                cardBrand = brand,
                cvc = ""
            )

            val actual = state.updateCvc("55")
            assertThat(actual).isEqualTo(
                CvcState(
                    cardBrand = brand,
                    cvc = "55"
                )
            )
            assertThat(actual.isValid).isEqualTo(false)
        }
    }

    @Test
    fun `cvc should be valid for cvc equals to cvc count - non-amex`() {
        nonAmexCards().forEach { brand ->
            val state = CvcState(
                cardBrand = brand,
                cvc = ""
            )

            val actual = state.updateCvc("555")
            assertThat(actual).isEqualTo(
                CvcState(
                    cardBrand = brand,
                    cvc = "555"
                )
            )
            assertThat(actual.isValid).isEqualTo(true)
        }
    }

    @Test
    fun `cvc should ignore cvc values greater than max cvc count - amex`() {
        val state = CvcState(
            cardBrand = CardBrand.AmericanExpress,
            cvc = "5555"
        )

        val actual = state.updateCvc("55555")
        assertThat(actual).isEqualTo(
            CvcState(
                cardBrand = CardBrand.AmericanExpress,
                cvc = "5555"
            )
        )
    }

    @Test
    fun `cvc should ignore cvc values greater than max cvc count - non-amex`() {
        nonAmexCards().forEach { brand ->
            val state = CvcState(
                cardBrand = brand,
                cvc = "555"
            )

            val actual = state.updateCvc("5555")
            assertThat(actual).isEqualTo(
                CvcState(
                    cardBrand = brand,
                    cvc = "555"
                )
            )
        }
    }

    private fun nonAmexCards(): List<CardBrand> {
        return CardBrand.orderedBrands.filter { it != CardBrand.AmericanExpress }
    }
}

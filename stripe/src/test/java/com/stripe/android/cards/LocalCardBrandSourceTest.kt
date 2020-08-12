package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.CardBrand
import kotlin.test.Test

class LocalCardBrandSourceTest {

    private val source = LocalCardBrandSource()

    @Test
    fun `getCardBrand should return expected brand`() {
        assertThat(source.getCardBrand("4"))
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getCardBrand("424242"))
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getCardBrand(CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getCardBrand(CardNumberFixtures.VISA_DEBIT_NO_SPACES))
            .isEqualTo(CardBrand.Visa)

        assertThat(source.getCardBrand("2221"))
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getCardBrand("2720"))
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getCardBrand("51"))
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getCardBrand("55"))
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getCardBrand(CardNumberFixtures.MASTERCARD_NO_SPACES))
            .isEqualTo(CardBrand.MasterCard)

        assertThat(source.getCardBrand("37"))
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getCardBrand("370000"))
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getCardBrand(CardNumberFixtures.AMEX_NO_SPACES))
            .isEqualTo(CardBrand.AmericanExpress)

        assertThat(source.getCardBrand("60"))
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getCardBrand("600000"))
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getCardBrand(CardNumberFixtures.DISCOVER_NO_SPACES))
            .isEqualTo(CardBrand.Discover)

        assertThat(source.getCardBrand("3528"))
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getCardBrand("3589"))
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getCardBrand(CardNumberFixtures.JCB_NO_SPACES))
            .isEqualTo(CardBrand.JCB)

        assertThat(source.getCardBrand("36"))
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getCardBrand("300"))
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getCardBrand("3095"))
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getCardBrand("38"))
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getCardBrand(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES))
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getCardBrand(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
            .isEqualTo(CardBrand.DinersClub)

        assertThat(source.getCardBrand("1"))
            .isEqualTo(CardBrand.Unknown)
        assertThat(source.getCardBrand("61"))
            .isEqualTo(CardBrand.Unknown)
    }
}

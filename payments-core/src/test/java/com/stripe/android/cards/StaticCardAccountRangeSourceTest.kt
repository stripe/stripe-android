package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class StaticCardAccountRangeSourceTest {
    private val source = StaticCardAccountRangeSource()

    @Test
    fun `getAccountRange() should return expected AccountRange`() = runTest {
        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("4"))?.brand
        ).isEqualTo(CardBrand.Visa)
        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("424242"))?.brand
        )
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getAccountRange(CardNumberFixtures.VISA)?.brand)
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getAccountRange(CardNumberFixtures.VISA_DEBIT)?.brand)
            .isEqualTo(CardBrand.Visa)

        assertThat(source.getAccountRange(CardNumber.Unvalidated("2221"))?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("2720"))?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("51"))?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("55"))?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange(CardNumberFixtures.MASTERCARD)?.brand)
            .isEqualTo(CardBrand.MasterCard)

        assertThat(source.getAccountRange(CardNumber.Unvalidated("37"))?.brand)
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("370000"))?.brand)
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getAccountRange(CardNumberFixtures.AMEX)?.brand)
            .isEqualTo(CardBrand.AmericanExpress)

        assertThat(source.getAccountRange(CardNumber.Unvalidated("60"))?.brand)
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("600000"))?.brand)
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getAccountRange(CardNumberFixtures.DISCOVER)?.brand)
            .isEqualTo(CardBrand.Discover)

        assertThat(source.getAccountRange(CardNumber.Unvalidated("3528"))?.brand)
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("3589"))?.brand)
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getAccountRange(CardNumberFixtures.JCB)?.brand)
            .isEqualTo(CardBrand.JCB)

        assertThat(source.getAccountRange(CardNumber.Unvalidated("36"))?.brand)
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getAccountRange(CardNumber.Unvalidated("300"))?.brand)
            .isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("3095"))?.brand
        ).isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("38"))?.brand
        ).isEqualTo(CardBrand.DinersClub)

        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_14)?.brand
        ).isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_14)?.panLength
        ).isEqualTo(14)

        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_16)?.brand
        ).isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_16)?.panLength
        ).isEqualTo(16)

        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("1"))
        ).isNull()
        assertThat(
            source.getAccountRange(CardNumber.Unvalidated("61"))
        ).isNull()
    }

    @Test
    fun `all BinRange values should be the expected length`() {
        assertThat(
            DefaultStaticCardAccountRanges.ACCOUNTS
                .all {
                    it.binRange.low.length == it.panLength &&
                        it.binRange.high.length == it.panLength
                }
        ).isTrue()
    }
}

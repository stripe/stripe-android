package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.CardBrand
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
internal class LocalCardAccountRangeSourceTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val source = LocalCardAccountRangeSource()

    @Test
    fun `getAccountRange() should return expected AccountRange`() = testDispatcher.runBlockingTest {
        assertThat(source.getAccountRange("4")?.brand)
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getAccountRange("424242")?.brand)
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getAccountRange(CardNumberFixtures.VISA_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.Visa)
        assertThat(source.getAccountRange(CardNumberFixtures.VISA_DEBIT_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.Visa)

        assertThat(source.getAccountRange("2221")?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange("2720")?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange("51")?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange("55")?.brand)
            .isEqualTo(CardBrand.MasterCard)
        assertThat(source.getAccountRange(CardNumberFixtures.MASTERCARD_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.MasterCard)

        assertThat(source.getAccountRange("37")?.brand)
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getAccountRange("370000")?.brand)
            .isEqualTo(CardBrand.AmericanExpress)
        assertThat(source.getAccountRange(CardNumberFixtures.AMEX_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.AmericanExpress)

        assertThat(source.getAccountRange("60")?.brand)
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getAccountRange("600000")?.brand)
            .isEqualTo(CardBrand.Discover)
        assertThat(source.getAccountRange(CardNumberFixtures.DISCOVER_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.Discover)

        assertThat(source.getAccountRange("3528")?.brand)
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getAccountRange("3589")?.brand)
            .isEqualTo(CardBrand.JCB)
        assertThat(source.getAccountRange(CardNumberFixtures.JCB_NO_SPACES)?.brand)
            .isEqualTo(CardBrand.JCB)

        assertThat(source.getAccountRange("36")?.brand)
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getAccountRange("300")?.brand)
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getAccountRange("3095")?.brand)
            .isEqualTo(CardBrand.DinersClub)
        assertThat(source.getAccountRange("38")?.brand)
            .isEqualTo(CardBrand.DinersClub)

        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)?.brand
        ).isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)?.panLength
        ).isEqualTo(14)

        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)?.brand
        )
            .isEqualTo(CardBrand.DinersClub)
        assertThat(
            source.getAccountRange(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)?.panLength
        ).isEqualTo(16)

        assertThat(source.getAccountRange("1"))
            .isNull()
        assertThat(source.getAccountRange("61"))
            .isNull()
    }

    @Test
    fun `all BinRange values should be the expected length`() {
        assertThat(
            LocalCardAccountRangeSource.ACCOUNTS
                .all {
                    it.binRange.low.length == it.panLength &&
                        it.binRange.high.length == it.panLength
                }
        ).isTrue()
    }
}

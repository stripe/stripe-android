package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import kotlin.test.Test

class DefaultStaticCardAccountRangesTest {

    @Test
    fun `filter with matching accounts should return non-empty`() {
        assertThat(
            DefaultStaticCardAccountRanges().filter(
                com.stripe.android.ui.core.elements.CardNumber.Unvalidated("6")
            )
        ).hasSize(4)
    }

    @Test
    fun `first with matching accounts should return expected value`() {
        assertThat(
            DefaultStaticCardAccountRanges().first(
                com.stripe.android.ui.core.elements.CardNumber.Unvalidated("6")
            )
        ).isEqualTo(
            AccountRange(
                binRange = BinRange(low = "6000000000000000", high = "6099999999999999"),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Discover,
                country = null
            )
        )
    }

    @Test
    fun `filter with no matching accounts should return empty`() {
        assertThat(
            DefaultStaticCardAccountRanges().filter(
                com.stripe.android.ui.core.elements.CardNumber.Unvalidated("9")
            )
        ).isEmpty()
    }

    @Test
    fun `first with no matching accounts should return null`() {
        assertThat(
            DefaultStaticCardAccountRanges().first(
                com.stripe.android.ui.core.elements.CardNumber.Unvalidated("9")
            )
        ).isNull()
    }
}

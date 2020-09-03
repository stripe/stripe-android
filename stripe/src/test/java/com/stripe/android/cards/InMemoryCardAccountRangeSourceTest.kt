package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
internal class InMemoryCardAccountRangeSourceTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val inMemoryCardAccountRangeSource = InMemoryCardAccountRangeSource(FakeStore())

    @Test
    fun `getAccountRange() should return expected AccountRange`() = testDispatcher.runBlockingTest {
        assertThat(
            inMemoryCardAccountRangeSource
                .getAccountRange(CardNumberFixtures.VISA)
        ).isEqualTo(
            AccountRange(
                binRange = BinRange(
                    low = "4242424240000000",
                    high = "4242424249999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
                country = "GB"
            )
        )
    }

    private class FakeStore : CardAccountRangeStore {
        override suspend fun get(bin: Bin) = AccountRangeFixtures.DEFAULT

        override fun save(bin: Bin, accountRanges: List<AccountRange>) {
        }

        override suspend fun contains(bin: Bin): Boolean = false
    }
}

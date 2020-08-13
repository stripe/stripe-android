package com.stripe.android.cards

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultCardAccountRangeStoreTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val store = DefaultCardAccountRangeStore(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `cache hit should return expected results`() = testDispatcher.runBlockingTest {
        store.save("424242", AccountRangeFixtures.DEFAULT)
        store.save("555555", BRANDX_ACCOUNT_RANGES)

        assertThat(store.get("424242"))
            .isEqualTo(AccountRangeFixtures.DEFAULT)

        assertThat(store.get("555555"))
            .isEqualTo(BRANDX_ACCOUNT_RANGES)
    }

    @Test
    fun `cache miss should return empty`() = testDispatcher.runBlockingTest {
        assertThat(store.get("999999"))
            .isEmpty()
    }

    private companion object {
        private val BRANDX_ACCOUNT_RANGES = listOf(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "5555550000000000",
                    high = "5555559999999999"
                ),
                panLength = 16,
                brandName = "BrandX",
                brand = CardBrand.Unknown
            )
        )
    }
}

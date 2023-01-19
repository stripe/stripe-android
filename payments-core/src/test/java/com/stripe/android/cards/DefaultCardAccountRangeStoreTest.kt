package com.stripe.android.cards

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultCardAccountRangeStoreTest {

    private val store = DefaultCardAccountRangeStore(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `cache hit should return expected results`() = runTest {
        store.save(BinFixtures.VISA, AccountRangeFixtures.DEFAULT)
        store.save(BinFixtures.FAKE, BRANDX_ACCOUNT_RANGES)

        assertThat(store.get(BinFixtures.VISA))
            .isEqualTo(AccountRangeFixtures.DEFAULT)

        assertThat(store.get(BinFixtures.FAKE))
            .isEqualTo(BRANDX_ACCOUNT_RANGES)
    }

    @Test
    fun `cache miss should return empty`() = runTest {
        assertThat(store.get(BinFixtures.FAKE))
            .isEmpty()
    }

    @Test
    fun `createPrefKey should return expected value`() {
        assertThat(
            store.createPrefKey(BinFixtures.VISA)
        ).isEqualTo("key_account_ranges:424242")
    }

    private companion object {
        private val BRANDX_ACCOUNT_RANGES = listOf(
            AccountRange(
                binRange = BinRange(
                    low = "9999990000000000",
                    high = "9999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.JCB
            )
        )
    }
}

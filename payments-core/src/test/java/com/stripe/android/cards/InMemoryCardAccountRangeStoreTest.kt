package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.BinFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InMemoryCardAccountRangeStoreTest {
    @Test
    fun `should be able to retrieve saved account ranges for BIN`() = runTest {
        val store = InMemoryCardAccountRangeStore()

        store.save(BinFixtures.VISA, listOf(AccountRangeFixtures.VISA))

        assertThat(
            store.get(BinFixtures.VISA)
        ).containsExactly(AccountRangeFixtures.VISA)
    }

    @Test
    fun `two instances of 'InMemoryCardAccountRangeStore' should maintain their own data`() = runTest {
        val store1 = InMemoryCardAccountRangeStore()
        val store2 = InMemoryCardAccountRangeStore()

        store1.save(BinFixtures.VISA, listOf(AccountRangeFixtures.VISA))
        store2.save(BinFixtures.VISA, listOf(AccountRangeFixtures.UNIONPAY16))

        assertThat(
            store1.get(BinFixtures.VISA)
        ).containsExactly(AccountRangeFixtures.VISA)

        assertThat(
            store2.get(BinFixtures.VISA)
        ).containsExactly(AccountRangeFixtures.UNIONPAY16)
    }
}

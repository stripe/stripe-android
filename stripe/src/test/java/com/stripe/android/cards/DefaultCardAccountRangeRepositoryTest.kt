package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.CardMetadata
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
internal class DefaultCardAccountRangeRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `getAccountRange() should return null`() = testDispatcher.runBlockingTest {
        assertThat(
            DefaultCardAccountRangeRepository(
                localCardAccountRangeSource = FakeLocalCardAccountRangeSource(),
                remoteCardAccountRangeSource = FakeRemoteCardAccountRangeSource()
            ).getAccountRange(CardNumberFixtures.VISA_NO_SPACES)
        ).isNull()
    }

    private class FakeLocalCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange? {
            return null
        }
    }

    private class FakeRemoteCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange? {
            return null
        }
    }
}

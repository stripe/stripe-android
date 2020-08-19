package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.CardNumberFixtures
import com.stripe.android.StripeRepository
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
internal class RemoteCardAccountRangeSourceTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val cardAccountRangeStore = mock<CardAccountRangeStore>()

    @Test
    fun `getAccountRange() should return expected AccountRange`() = testDispatcher.runBlockingTest {
        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            FakeStripeRepository(VISA_METADATA),
            REQUEST_OPTIONS,
            cardAccountRangeStore
        )

        assertThat(
            remoteCardAccountRangeSource.getAccountRange(
                CardNumberFixtures.VISA_NO_SPACES
            )
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "4242424240000000",
                    high = "4242424249999999"
                ),
                panLength = 16,
                brandInfo = CardMetadata.AccountRange.BrandInfo.Visa,
                country = "GB"
            )
        )
        verify(cardAccountRangeStore).save(
            BinFixtures.VISA,
            AccountRangeFixtures.DEFAULT
        )
    }

    @Test
    fun `getAccountRange() when CardMetadata is empty should return null`() = testDispatcher.runBlockingTest {
        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            FakeStripeRepository(EMPTY_METADATA),
            REQUEST_OPTIONS,
            cardAccountRangeStore
        )

        assertThat(
            remoteCardAccountRangeSource.getAccountRange(
                CardNumberFixtures.VISA_NO_SPACES
            )
        ).isNull()
        verify(cardAccountRangeStore).save(
            BinFixtures.VISA,
            emptyList()
        )
    }

    @Test
    fun `getAccountRange() when card number is less than required BIN length should return null`() = testDispatcher.runBlockingTest {
        val repository = mock<StripeRepository>()

        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            repository,
            REQUEST_OPTIONS,
            cardAccountRangeStore
        )

        assertThat(
            remoteCardAccountRangeSource.getAccountRange("42")
        ).isNull()

        verify(repository, never()).getCardMetadata(any(), any())
        verify(cardAccountRangeStore, never()).save(any(), any())
    }

    private class FakeStripeRepository(
        private val cardMetadata: CardMetadata
    ) : AbsFakeStripeRepository() {
        override suspend fun getCardMetadata(
            bin: Bin,
            options: ApiRequest.Options
        ) = cardMetadata
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )

        private val EMPTY_METADATA = CardMetadata(
            bin = BinFixtures.FAKE,
            accountRanges = emptyList()
        )

        private val VISA_METADATA = CardMetadata(
            bin = BinFixtures.VISA,
            accountRanges = AccountRangeFixtures.DEFAULT
        )
    }
}

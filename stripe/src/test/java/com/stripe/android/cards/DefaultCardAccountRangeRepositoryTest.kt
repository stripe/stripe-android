package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class DefaultCardAccountRangeRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val realStore = DefaultCardAccountRangeStore(application)
    private val realRepository = createRealRepository(realStore)

    @Test
    fun `repository with real sources returns expected results`() = runBlocking {
        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("42424")
            )
        ).isNull()

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.VISA)
        ).isEqualTo(
            AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa
            )
        )
        assertThat(realStore.get(BinFixtures.VISA))
            .hasSize(2)

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.DINERS_CLUB_14)
        ).isEqualTo(
            AccountRangeFixtures.DINERSCLUB14
        )
        assertThat(
            realStore.get(BinFixtures.DINERSCLUB14)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.DINERS_CLUB_16)
        ).isEqualTo(
            AccountRangeFixtures.DINERSCLUB16
        )
        assertThat(
            realStore.get(BinFixtures.DINERSCLUB16)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("378282")
            )
        ).isEqualTo(
            AccountRangeFixtures.AMERICANEXPRESS
        )

        assertThat(
            realStore.get(BinFixtures.AMEX)
        ).hasSize(1)

        assertThat(
            realRepository.getAccountRange(CardNumber.Unvalidated("5555552500001001"))
        ).isEqualTo(
            AccountRangeFixtures.MASTERCARD
        )
        assertThat(
            realStore.get(BinFixtures.MASTERCARD)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("356840")
            )
        ).isEqualTo(
            AccountRangeFixtures.JCB
        )
        assertThat(
            realStore.get(BinFixtures.JCB)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("621682")
            )
        ).isEqualTo(
            AccountRangeFixtures.UNIONPAY19
        )

        assertThat(
            realStore.get(BinFixtures.UNIONPAY_CN)
        ).hasSize(69)
    }

    @Test
    fun `getAccountRange() should return null`() = testDispatcher.runBlockingTest {
        assertThat(
            DefaultCardAccountRangeRepository(
                inMemorySource = FakeCardAccountRangeSource(),
                remoteSource = FakeCardAccountRangeSource(),
                staticSource = FakeCardAccountRangeSource(),
                store = realStore
            ).getAccountRange(CardNumberFixtures.VISA)
        ).isNull()
    }

    @Test
    fun `loading when no sources are loading should emit false`() = testDispatcher.runBlockingTest {
        val collected = mutableListOf<Boolean>()
        DefaultCardAccountRangeRepository(
            inMemorySource = FakeCardAccountRangeSource(),
            remoteSource = FakeCardAccountRangeSource(),
            staticSource = FakeCardAccountRangeSource(),
            store = realStore
        ).loading.collect {
            collected.add(it)
        }

        assertThat(collected)
            .containsExactly(false)
    }

    @Test
    fun `loading when one source is loading should emit true`() = testDispatcher.runBlockingTest {
        val collected = mutableListOf<Boolean>()
        DefaultCardAccountRangeRepository(
            inMemorySource = FakeCardAccountRangeSource(),
            remoteSource = FakeCardAccountRangeSource(isLoading = true),
            staticSource = FakeCardAccountRangeSource(),
            store = realStore
        ).loading.collect {
            collected.add(it)
        }

        assertThat(collected)
            .containsExactly(true)
    }

    @Test
    fun `getAccountRange should not access remote source if BIN is in store`() = testDispatcher.runBlockingTest {
        val remoteSource = mock<CardAccountRangeSource>()
        val repository = DefaultCardAccountRangeRepository(
            inMemorySource = FakeCardAccountRangeSource(),
            remoteSource = remoteSource,
            staticSource = FakeCardAccountRangeSource(),
            store = realStore
        )

        val bin = requireNotNull(CardNumberFixtures.VISA.bin)
        realStore.save(bin, emptyList())

        // should not access remote source
        repository.getAccountRange(CardNumberFixtures.VISA)
        verify(remoteSource, never()).getAccountRange(CardNumberFixtures.VISA)
    }

    private fun createRealRepository(
        store: CardAccountRangeStore
    ): CardAccountRangeRepository {
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = createRemoteCardAccountRangeSource(store),
            staticSource = StaticCardAccountRangeSource(),
            store = realStore
        )
    }

    private fun createRemoteCardAccountRangeSource(
        store: CardAccountRangeStore,
        publishableKey: String = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    ): CardAccountRangeSource {
        val stripeRepository = StripeApiRepository(
            application,
            publishableKey
        )
        return RemoteCardAccountRangeSource(
            stripeRepository,
            ApiRequest.Options(publishableKey),
            store,
            { },
            AnalyticsRequest.Factory(),
            AnalyticsDataFactory(application, publishableKey)
        )
    }

    private class FakeCardAccountRangeSource(
        isLoading: Boolean = false
    ) : CardAccountRangeSource {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? {
            return null
        }

        override val loading: Flow<Boolean> = flowOf(isLoading)
    }
}

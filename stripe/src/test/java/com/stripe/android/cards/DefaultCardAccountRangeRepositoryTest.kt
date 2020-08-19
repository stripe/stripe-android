package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.CardNumberFixtures
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
            realRepository.getAccountRange("42424")
        ).isNull()

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.VISA_NO_SPACES)
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = CardMetadata.AccountRange.BrandInfo.Visa
            )
        )
        assertThat(realStore.get(BinFixtures.VISA))
            .hasSize(2)

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "36000000000000",
                    high = "36999999999999"
                ),
                panLength = 14,
                brandInfo = CardMetadata.AccountRange.BrandInfo.DinersClub
            )
        )
        assertThat(
            realStore.get(BinFixtures.DINERSCLUB14)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "3000000000000000",
                    high = "3059999999999999"
                ),
                panLength = 16,
                brandInfo = CardMetadata.AccountRange.BrandInfo.DinersClub
            )
        )
        assertThat(
            realStore.get(BinFixtures.DINERSCLUB16)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange("378282")
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "378282000000000",
                    high = "378282999999999"
                ),
                panLength = 15,
                brandInfo = CardMetadata.AccountRange.BrandInfo.AmericanExpress,
                country = "US"
            )
        )

        assertThat(
            realStore.get(BinFixtures.AMEX)
        ).hasSize(1)

        assertThat(
            realRepository.getAccountRange("5555552500001001")
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "5100000000000000",
                    high = "5599999999999999"
                ),
                panLength = 16,
                brandInfo = CardMetadata.AccountRange.BrandInfo.Mastercard
            )
        )
        assertThat(
            realStore.get(BinFixtures.MASTERCARD)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange("356840")
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "3528000000000000",
                    high = "3589999999999999"
                ),
                panLength = 16,
                brandInfo = CardMetadata.AccountRange.BrandInfo.JCB
            )
        )
        assertThat(
            realStore.get(BinFixtures.JCB)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange("621682")
        ).isEqualTo(
            CardMetadata.AccountRange(
                binRange = BinRange(
                    low = "6216828050000000000",
                    high = "6216828059999999999"
                ),
                panLength = 19,
                brandInfo = CardMetadata.AccountRange.BrandInfo.UnionPay,
                country = "CN"
            )
        )

        assertThat(
            realStore.get(BinFixtures.UNIONPAY_CN)
        ).hasSize(69)
    }

    @Test
    fun `getAccountRange() should return null`() = testDispatcher.runBlockingTest {
        assertThat(
            DefaultCardAccountRangeRepository(
                inMemoryCardAccountRangeSource = FakeCardAccountRangeSource(),
                localCardAccountRangeSource = FakeCardAccountRangeSource(),
                remoteCardAccountRangeSource = FakeCardAccountRangeSource()
            ).getAccountRange(CardNumberFixtures.VISA_NO_SPACES)
        ).isNull()
    }

    private fun createRealRepository(
        store: CardAccountRangeStore
    ): CardAccountRangeRepository {
        val stripeRepository = StripeApiRepository(
            application,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        return DefaultCardAccountRangeRepository(
            inMemoryCardAccountRangeSource = InMemoryCardAccountRangeSource(store),
            localCardAccountRangeSource = LocalCardAccountRangeSource(),
            remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                stripeRepository,
                ApiRequest.Options(
                    ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
                ),
                store
            )
        )
    }

    private class FakeCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange? {
            return null
        }
    }
}

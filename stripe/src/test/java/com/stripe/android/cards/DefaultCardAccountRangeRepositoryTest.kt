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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                inMemoryCardAccountRangeSource = FakeCardAccountRangeSource(),
                remoteCardAccountRangeSource = FakeCardAccountRangeSource(),
                staticCardAccountRangeSource = FakeCardAccountRangeSource()
            ).getAccountRange(CardNumberFixtures.VISA)
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
            remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                stripeRepository,
                ApiRequest.Options(
                    ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
                ),
                store
            ),
            staticCardAccountRangeSource = StaticCardAccountRangeSource()
        )
    }

    private class FakeCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): CardMetadata.AccountRange? {
            return null
        }
    }
}

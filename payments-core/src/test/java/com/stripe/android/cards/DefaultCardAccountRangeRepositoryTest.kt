package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultCardAccountRangeRepositoryTest {

    @get:Rule
    val networkRule = NetworkRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val realStore = DefaultCardAccountRangeStore(application)
    private val realRepository = createRealRepository(realStore)

    @Ignore("Failing. See https://jira.corp.stripe.com/browse/RUN_MOBILESDK-1661")
    @Suppress("LongMethod")
    @Test
    fun `repository with real sources returns expected results`(): Unit = runBlocking {
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
        ).containsExactly(
            AccountRange(
                binRange = BinRange(low = "5555550070000000", high = "5555550089999999"),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Mastercard,
                country = "BR"
            )
        )

        assertThat(
            realStore.get(BinFixtures.JCB)
        ).isEmpty()

        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("601100")
            )
        ).isEqualTo(
            AccountRangeFixtures.DISCOVER
        )
        assertThat(
            realStore.get(BinFixtures.DISCOVER)
        ).containsExactly(
            AccountRange(
                binRange = BinRange(low = "6011000000000000", high = "6011011999999999"),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Discover,
                country = "US"
            )
        )

        assertThat(
            realRepository.getAccountRange(
                CardNumber.Unvalidated("356840")
            )
        ).isEqualTo(
            AccountRangeFixtures.UNIONPAY16
        )
        assertThat(
            realStore.get(BinFixtures.UNIONPAY16)
        ).containsExactly(
            AccountRange(
                binRange = BinRange(low = "3568400000000000", high = "3568409999999999"),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.UnionPay,
                country = "CN"
            )
        )
    }

    @Test
    fun `getAccountRange() should return null`() = runTest {
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
    fun `loading when no sources are loading should emit false`() = runTest {
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
    fun `loading when one source is loading should emit true`() = runTest {
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
    fun `Should return local account ranges if BIN is in store`() = runTest {
        val accountRanges = FAKE_ACCOUNT_RANGES.shuffled()

        val repository = DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(realStore),
            remoteSource = FakeCardAccountRangeSource(),
            staticSource = StaticCardAccountRangeSource(),
            store = realStore,
        )

        val bin = requireNotNull(CardNumberFixtures.VISA.bin)
        realStore.save(bin, accountRanges)

        val ranges = repository.getAccountRanges(CardNumberFixtures.VISA)
        assertThat(ranges).containsExactlyElementsIn(accountRanges)
    }

    @Test
    fun `Should return static account ranges if remote source fails and BIN is not in store`() = runTest {
        val repository = DefaultCardAccountRangeRepository(
            inMemorySource = FakeCardAccountRangeSource(accountRanges = null),
            remoteSource = mock(),
            staticSource = StaticCardAccountRangeSource(),
            store = realStore,
        )

        val ranges = repository.getAccountRanges(CardNumberFixtures.VISA)
        assertThat(ranges).containsExactlyElementsIn(VISA_ACCOUNT_RANGES)
    }

    @Test
    fun `Should load from remote source if BIN is not in store`() = runTest {
        val accountRanges = FAKE_ACCOUNT_RANGES.shuffled()
        val remoteSource = FakeCardAccountRangeSource(accountRanges = accountRanges)

        val repository = DefaultCardAccountRangeRepository(
            inMemorySource = FakeCardAccountRangeSource(),
            remoteSource = remoteSource,
            staticSource = FakeCardAccountRangeSource(),
            store = realStore,
        )

        val ranges = repository.getAccountRanges(CardNumberFixtures.VISA)
        assertThat(ranges).containsExactlyElementsIn(accountRanges)
    }

    @Test
    fun `getAccountRanges sends all parameters`() = runTest {
        val binPrefix = "513130"
        networkRule.enqueue(
            method("GET"),
            path("/edge-internal/card-metadata"),
            header("Authorization", "Bearer ${DEFAULT_OPTIONS.apiKey}"),
            header("User-Agent", "Stripe/v1 ${StripeSdkVersion.VERSION}"),
            query("bin_prefix", binPrefix),
        ) { response ->
            response.setBody(
                """
                    {
                        "data":[
                            {
                                "account_range_high":"5131309999999999",
                                "account_range_low":"5131300000000000",
                                "brand":"MASTERCARD",
                                "country":"FR",
                                "funding":"DEBIT",
                                "pan_length":16
                            },
                            {
                                "account_range_high":"5131304799999999",
                                "account_range_low":"5131304700000000",
                                "brand":"CARTES_BANCAIRES",
                                "country":"FR",
                                "funding":"DEBIT",
                                "pan_length":16
                            }
                        ]
                    }
                """.trimIndent()
            )
        }

        val accountRanges = realRepository.getAccountRanges(CardNumber.Unvalidated(binPrefix))
        val cardBrands = accountRanges?.map { it.brand }?.toSet()

        assertThat(accountRanges).isNotEmpty()
        assertThat(cardBrands).containsExactly(CardBrand.MasterCard, CardBrand.CartesBancaires)
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
            { publishableKey }
        )
        return RemoteCardAccountRangeSource(
            stripeRepository,
            ApiRequest.Options(publishableKey),
            store,
            { },
            PaymentAnalyticsRequestFactory(application, publishableKey)
        )
    }

    private class FakeCardAccountRangeSource(
        isLoading: Boolean = false,
        private val accountRanges: List<AccountRange>? = null,
    ) : CardAccountRangeSource {
        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): List<AccountRange>? {
            return accountRanges
        }

        override val loading: Flow<Boolean> = flowOf(isLoading)
    }

    private companion object {
        private val DEFAULT_OPTIONS = ApiRequest.Options("pk_test_vOo1umqsYxSrP5UXfOeL3ecm")

        private val VISA_ACCOUNT_RANGES = listOf(
            AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
            )
        )

        private val FAKE_ACCOUNT_RANGES = listOf(
            AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
            ),
            AccountRange(
                binRange = BinRange(
                    low = "2221000000000000",
                    high = "2720999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Mastercard,
            ),
            AccountRange(
                BinRange(
                    low = "5100000000000000",
                    high = "5599999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Mastercard,
            ),
        )
    }
}

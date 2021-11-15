package com.stripe.android.cards

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class RemoteCardAccountRangeSourceTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val cardAccountRangeStore = mock<CardAccountRangeStore>()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `getAccountRange() should return expected AccountRange`() = testDispatcher.runBlockingTest {
        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            FakeStripeRepository(VISA_METADATA),
            REQUEST_OPTIONS,
            cardAccountRangeStore,
            { },
            PaymentAnalyticsRequestFactory(
                ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
            )
        )

        assertThat(
            remoteCardAccountRangeSource.getAccountRange(
                CardNumberFixtures.VISA
            )
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
        verify(cardAccountRangeStore).save(
            BinFixtures.VISA,
            AccountRangeFixtures.DEFAULT
        )
    }

    @Test
    fun `getAccountRange() when CardMetadata is empty should return null`() =
        testDispatcher.runBlockingTest {
            val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                FakeStripeRepository(EMPTY_METADATA),
                REQUEST_OPTIONS,
                cardAccountRangeStore,
                { },
                PaymentAnalyticsRequestFactory(
                    ApplicationProvider.getApplicationContext(),
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
                )
            )

            assertThat(
                remoteCardAccountRangeSource.getAccountRange(
                    CardNumberFixtures.VISA
                )
            ).isNull()
            verify(cardAccountRangeStore).save(
                BinFixtures.VISA,
                emptyList()
            )
        }

    @Test
    fun `getAccountRange() when card number is less than required BIN length should return null`() =
        testDispatcher.runBlockingTest {
            val repository = mock<StripeRepository>()

            val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                repository,
                REQUEST_OPTIONS,
                cardAccountRangeStore,
                { },
                PaymentAnalyticsRequestFactory(
                    ApplicationProvider.getApplicationContext(),
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
                )
            )

            assertThat(
                remoteCardAccountRangeSource.getAccountRange(
                    CardNumber.Unvalidated("42")
                )
            ).isNull()

            verify(repository, never()).getCardMetadata(any(), any())
            verify(cardAccountRangeStore, never()).save(any(), any())
        }

    @Test
    fun `getAccountRange() should fire missing range analytics request when response is not empty but card number does not match`() =
        testDispatcher.runBlockingTest {
            val analyticsRequests = mutableListOf<AnalyticsRequest>()

            val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                FakeStripeRepository(
                    CardMetadata(
                        bin = BinFixtures.VISA,
                        accountRanges = listOf(
                            AccountRange(
                                binRange = BinRange(
                                    low = "4242420000000000",
                                    high = "4242424200000000"
                                ),
                                panLength = 16,
                                brandInfo = AccountRange.BrandInfo.Visa,
                                country = "GB"
                            )
                        )
                    )
                ),
                REQUEST_OPTIONS,
                cardAccountRangeStore,
                {
                    analyticsRequests.add(it)
                },
                PaymentAnalyticsRequestFactory(
                    ApplicationProvider.getApplicationContext(),
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
                )
            )

            remoteCardAccountRangeSource.getAccountRange(
                CardNumber.Unvalidated("4242424242424242")
            )

            assertThat(analyticsRequests)
                .hasSize(1)
            assertThat(analyticsRequests.first().params["event"])
                .isEqualTo("stripe_android.card_metadata_missing_range")
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

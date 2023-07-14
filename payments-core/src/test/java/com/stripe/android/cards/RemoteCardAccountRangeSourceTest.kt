package com.stripe.android.cards

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class RemoteCardAccountRangeSourceTest {
    private val cardAccountRangeStore = mock<CardAccountRangeStore>()

    @Test
    fun `getAccountRange() should return expected AccountRange`() = runTest {
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
    }

    @Test
    fun `getAccountRange() when CardMetadata is empty should return null`() =
        runTest {
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
        }

    @Test
    fun `getAccountRange() stores server response if not empty`() = runTest {
        val expectedRanges = AccountRangeFixtures.DEFAULT

        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            stripeRepository = FakeStripeRepository(VISA_METADATA),
            requestOptions = REQUEST_OPTIONS,
            cardAccountRangeStore = cardAccountRangeStore,
            analyticsRequestExecutor = {},
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
            )
        )

        remoteCardAccountRangeSource.getAccountRange(CardNumberFixtures.VISA)
        verify(cardAccountRangeStore).save(BinFixtures.VISA, expectedRanges)
    }

    @Test
    fun `getAccountRange() does store server response even if empty`() = runTest {
        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            stripeRepository = FakeStripeRepository(EMPTY_METADATA),
            requestOptions = REQUEST_OPTIONS,
            cardAccountRangeStore = cardAccountRangeStore,
            analyticsRequestExecutor = {},
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
            )
        )

        remoteCardAccountRangeSource.getAccountRange(CardNumberFixtures.VISA)
        verify(cardAccountRangeStore).save(BinFixtures.VISA, emptyList())
    }

    @Test
    fun `getAccountRange() does not store server response if the request failed`() = runTest {
        val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
            stripeRepository = FakeStripeRepository(FAILED_REQUEST),
            requestOptions = REQUEST_OPTIONS,
            cardAccountRangeStore = cardAccountRangeStore,
            analyticsRequestExecutor = {},
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
            )
        )

        remoteCardAccountRangeSource.getAccountRange(CardNumberFixtures.VISA)
        verify(cardAccountRangeStore, never()).save(any(), any())
    }

    @Test
    fun `getAccountRange() when card number is less than required BIN length should return null`() =
        runTest {
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
        runTest {
            val analyticsRequests = mutableListOf<AnalyticsRequest>()

            val remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                FakeStripeRepository(
                    Result.success(
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
        private val result: Result<CardMetadata>
    ) : AbsFakeStripeRepository() {
        override suspend fun getCardMetadata(
            bin: Bin,
            options: ApiRequest.Options
        ): Result<CardMetadata> = result
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )

        private val FAILED_REQUEST = Result.failure<CardMetadata>(IOException())

        private val EMPTY_METADATA = Result.success(
            CardMetadata(
                bin = BinFixtures.FAKE,
                accountRanges = emptyList()
            )
        )

        private val VISA_METADATA = Result.success(
            CardMetadata(
                bin = BinFixtures.VISA,
                accountRanges = AccountRangeFixtures.DEFAULT
            )
        )
    }
}

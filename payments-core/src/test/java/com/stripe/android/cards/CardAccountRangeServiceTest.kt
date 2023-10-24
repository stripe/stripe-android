package com.stripe.android.cards

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.ACCOUNTS
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.UNIONPAY16_ACCOUNTS
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.UNIONPAY19_ACCOUNTS
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.utils.TestUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class CardAccountRangeServiceTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val applicationContext =
        ApplicationProvider.getApplicationContext<Application>().applicationContext
    private val publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY

    @Test
    fun `test the card metadata service is called only for UnionPay cards if not CBC eligible`() = runTest {
        val unionPayAccounts = UNIONPAY16_ACCOUNTS + UNIONPAY19_ACCOUNTS

        unionPayAccounts.forEach {
            testIfRemoteCalled(isCbcEligible = false, it.binRange.low, expectedRemoteCall = true)
            testIfRemoteCalled(isCbcEligible = false, it.binRange.high, expectedRemoteCall = true)
        }

        ACCOUNTS
            .filterNot { unionPayAccounts.contains(it) }
            .forEach {
                testIfRemoteCalled(isCbcEligible = false, it.binRange.low, expectedRemoteCall = false)
                testIfRemoteCalled(isCbcEligible = false, it.binRange.high, expectedRemoteCall = false)
            }
    }

    @Test
    fun `test the card metadata service is always called if CBC eligible`() = runTest {
        ACCOUNTS.forEach {
            testIfRemoteCalled(isCbcEligible = true, it.binRange.low, expectedRemoteCall = true)
            testIfRemoteCalled(isCbcEligible = true, it.binRange.high, expectedRemoteCall = true)
        }
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private suspend fun testIfRemoteCalled(
        isCbcEligible: Boolean,
        cardNumberString: String,
        expectedRemoteCall: Boolean,
    ) {
        val mockRemoteCardAccountRangeSource = mock<CardAccountRangeSource>()
        val serviceMockRemote = CardAccountRangeService(
            createMockRemoteDefaultCardAccountRangeRepository(mockRemoteCardAccountRangeSource),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(accountRanges: List<AccountRange>) {
                }
            },
            isCbcEligible = { isCbcEligible },
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        serviceMockRemote.onCardNumberChanged(cardNumber)
        verify(
            mockRemoteCardAccountRangeSource,
            if (expectedRemoteCall) {
                times(1)
            } else {
                never()
            }
        ).getAccountRanges(cardNumber)
    }

    @Test
    fun `If CBC is disabled, return the matched brand as soon as there is input`() = runTest {
        val expectedAccountRange = AccountRange(
            binRange = BinRange(
                low = "4000000000000000",
                high = "4999999999999999",
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
        )

        val accountRanges = testBehavior(
            cardNumber = "4",
            isCbcEligible = false,
        )

        assertThat(accountRanges).containsExactly(expectedAccountRange)
    }

    @Test
    fun `If CBC is enabled, don't return a matched brand until 8 characters are entered`() = runTest {
        val accountRanges = testBehavior(
            cardNumber = "4",
            isCbcEligible = true,
        )

        assertThat(accountRanges).isEmpty()
    }

    @Test
    fun `If CBC is enabled, return the matched brands once 8 characters are entered`() = runTest {
        val expectedAccountRanges = listOf(
            AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999",
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
            ),
            AccountRange(
                binRange = BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999",
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.CartesBancaires,
            )
        )

        val accountRanges = testBehavior(
            cardNumber = "4000 0000",
            isCbcEligible = true,
            mockRemoteCardAccountRangeSource = object : CardAccountRangeSource {
                override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
                    return expectedAccountRanges
                }

                override val loading: Flow<Boolean> = flowOf(false)
            }
        )

        assertThat(accountRanges).containsExactlyElementsIn(expectedAccountRanges)
    }

    private suspend fun testBehavior(
        cardNumber: String,
        isCbcEligible: Boolean,
        mockRemoteCardAccountRangeSource: CardAccountRangeSource? = null,
    ): List<AccountRange> {
        val completable = CompletableDeferred<List<AccountRange>>()

        val repository = mockRemoteCardAccountRangeSource?.let {
            createMockRemoteDefaultCardAccountRangeRepository(it)
        } ?: createDefaultCardAccountRangeRepository()

        val service = CardAccountRangeService(
            cardAccountRangeRepository = repository,
            uiContext = testDispatcher,
            workContext = testDispatcher,
            staticCardAccountRanges = DefaultStaticCardAccountRanges(),
            accountRangeResultListener = object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(accountRanges: List<AccountRange>) {
                    completable.complete(accountRanges)
                }
            },
            isCbcEligible = { isCbcEligible },
        )

        service.onCardNumberChanged(CardNumber.Unvalidated(cardNumber))

        return completable.await()
    }

    private fun createMockRemoteDefaultCardAccountRangeRepository(
        mockRemoteCardAccountRangeSource: CardAccountRangeSource
    ): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(applicationContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = mockRemoteCardAccountRangeSource,
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }

    @Test
    fun `test card metadata service pan length`() = runTest {
        verifyRemotePanLength("6500079999999999999", 16)
    }

    private fun verifyRemotePanLength(cardNumberString: String, expectedPanLength: Int) {
        var panLength: Int? = null
        val latch = CountDownLatch(1)
        val serviceMockRemote = CardAccountRangeService(
            createDefaultCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(accountRanges: List<AccountRange>) {
                    val newAccountRange = accountRanges.firstOrNull()
                    panLength = newAccountRange?.panLength
                    latch.countDown()
                }
            },
            isCbcEligible = { false },
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        serviceMockRemote.onCardNumberChanged(cardNumber)
        latch.await(2, TimeUnit.SECONDS)
        TestUtils.idleLooper()

        assertThat(panLength).isEqualTo(expectedPanLength)
    }

    private fun createDefaultCardAccountRangeRepository(): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(applicationContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = createRemoteCardAccountRangeSource(),
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }

    private fun createRemoteCardAccountRangeSource(): CardAccountRangeSource {
        return RemoteCardAccountRangeSource(
            StripeApiRepository(applicationContext, { publishableKey }),
            ApiRequest.Options(publishableKey),
            DefaultCardAccountRangeStore(applicationContext),
            DefaultAnalyticsRequestExecutor(),
            PaymentAnalyticsRequestFactory(applicationContext, publishableKey)
        )
    }
}

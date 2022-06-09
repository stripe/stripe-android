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
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.utils.TestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@ExperimentalCoroutinesApi
class CardAccountRangeServiceTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val applicationContext =
        ApplicationProvider.getApplicationContext<Application>().applicationContext
    private val publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY

    @Test
    fun `test the card metadata service is called only for UnionPay cards`() = runTest {
        val setRemoteCheckedCards =
            UNIONPAY16_ACCOUNTS
                .plus(UNIONPAY19_ACCOUNTS)
        setRemoteCheckedCards.forEach {
            testIfRemoteCalled(it.binRange.low, true)
            testIfRemoteCalled(it.binRange.high, true)
        }
        ACCOUNTS
            .filterNot { setRemoteCheckedCards.contains(it) }
            .forEach {
                testIfRemoteCalled(it.binRange.low, false)
                testIfRemoteCalled(it.binRange.high, false)
            }
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private suspend fun testIfRemoteCalled(cardNumberString: String, expectedRemoteCall: Boolean) {
        val mockRemoteCardAccountRangeSource = mock<CardAccountRangeSource>()
        val serviceMockRemote = CardAccountRangeService(
            createMockRemoteDefaultCardAccountRangeRepository(mockRemoteCardAccountRangeSource),
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangeResult(newAccountRange: AccountRange?) {
                }
            }
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
        ).getAccountRange(cardNumber)
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
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangeResult(newAccountRange: AccountRange?) {
                    panLength = newAccountRange?.panLength
                    latch.countDown()
                }
            }
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

package com.stripe.android.cards

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.ACCOUNTS
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.CARTES_BANCAIRES_ACCOUNT_RANGES
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.UNIONPAY16_ACCOUNTS
import com.stripe.android.cards.DefaultStaticCardAccountRanges.Companion.UNIONPAY19_ACCOUNTS
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.RequestSurface
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeCardFundingFilter
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardAccountRangeServiceTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)
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
        ACCOUNTS
            .filterNot { CARTES_BANCAIRES_ACCOUNT_RANGES.contains(it) }
            .forEach {
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
        val mockRemoteCardAccountRangeSource = mock<CardAccountRangeSource> {
            on { loading } doReturn stateFlowOf(false)
        }

        val serviceMockRemote = DefaultCardAccountRangeService(
            createMockRemoteDefaultCardAccountRangeRepository(mockRemoteCardAccountRangeSource),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            cardFundingFilter = DefaultCardFundingFilter
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        serviceMockRemote.onCardNumberChanged(cardNumber, isCbcEligible = isCbcEligible)
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
    fun `If CBC is disabled and only one brand matches, return the matched brand as soon as there is input`() =
        runTest {
            val expectedAccountRange = AccountRange(
                binRange = BinRange(
                    low = "2221000000000000",
                    high = "2720999999999999",
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Mastercard,
                funding = CardFunding.Unknown,
            )

            testBehavior(
                cardNumber = "2",
                isCbcEligible = false,
            ) {
                assertThat(awaitItem().accountRanges).containsExactly(expectedAccountRange)
            }
        }

    @Test
    fun `If CBC is enabled, don't return a matched brand until 8 characters are entered`() = runTest {
        testBehavior(
            cardNumber = "4",
            isCbcEligible = true,
        ) {
            assertThat(awaitItem().accountRanges).isEmpty()
        }
    }

    @Test
    fun `If CBC is enabled, return the matched brands once 8 characters are entered`() = runTest {
        val expectedAccountRanges = listOf(
            defaultAccountRange(
                brandInfo = AccountRange.BrandInfo.Visa,
                funding = CardFunding.Unknown,
            ),
            defaultAccountRange(
                brandInfo = AccountRange.BrandInfo.CartesBancaires,
                funding = CardFunding.Unknown,
            )
        )

        testBehavior(
            cardNumber = "4000 0000",
            isCbcEligible = true,
            mockRemoteCardAccountRangeSource = object : CardAccountRangeSource {
                override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
                    return expectedAccountRanges
                }

                override val loading: StateFlow<Boolean> = stateFlowOf(false)
            }
        ) {
            assertThat(awaitItem().accountRanges).containsExactlyElementsIn(expectedAccountRanges)
        }
    }

    private suspend fun testBehavior(
        cardNumber: String,
        isCbcEligible: Boolean,
        mockRemoteCardAccountRangeSource: CardAccountRangeSource? = null,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
        validate: suspend TurbineTestContext<CardAccountRangeService.AccountRangesResult>.() -> Unit,
    ) {
        val repository = mockRemoteCardAccountRangeSource?.let {
            createMockRemoteDefaultCardAccountRangeRepository(it)
        } ?: createDefaultCardAccountRangeRepository()

        val service = DefaultCardAccountRangeService(
            cardAccountRangeRepository = repository,
            uiContext = testDispatcher,
            workContext = testDispatcher,
            staticCardAccountRanges = DefaultStaticCardAccountRanges(),
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter
        )

        service.onCardNumberChanged(CardNumber.Unvalidated(cardNumber), isCbcEligible = isCbcEligible)

        service.accountRangeResultFlow.test {
            validate(this)
        }
    }

    @Test
    fun `Brands that are disallowed by the card brand filter are filtered out`() =
        runTest {
            // Disallow Mastercard
            val disallowedBrands = setOf(CardBrand.MasterCard)
            val cardBrandFilter = FakeCardBrandFilter(disallowedBrands)

            // Use a card number that matches Mastercard (starts with '2')
            val cardNumber = "2"

            // Call testBehavior with the custom CardBrandFilter
            testBehavior(
                cardNumber = cardNumber,
                isCbcEligible = false,
                cardBrandFilter = cardBrandFilter
            ) {
                val result = awaitItem()
                // Since Mastercard is disallowed, the accountRanges should be empty
                assertThat(result.accountRanges).isEmpty()
                // Even though mastercard is disallowed, it should be in the unfiltered ranges
                assertThat(result.unfilteredAccountRanges).hasSize(1)
                assertThat(result.unfilteredAccountRanges.firstOrNull()?.brand).isEqualTo(CardBrand.MasterCard)
            }
        }

    @Test
    fun `Brands that are not disallowed by the card brand filter are filtered out`() =
        runTest {
            // Disallow Visa
            val disallowedBrands = setOf(CardBrand.Visa)
            val cardBrandFilter = FakeCardBrandFilter(disallowedBrands)

            // Use a card number that matches Mastercard (starts with '2')
            val cardNumber = "2"

            // Call testBehavior with the custom CardBrandFilter
            testBehavior(
                cardNumber = cardNumber,
                isCbcEligible = false,
                cardBrandFilter = cardBrandFilter
            ) {
                assertThat(awaitItem().accountRanges).isNotEmpty()
            }
        }

    @Test
    fun `test card metadata service pan length`() = runTest {
        verifyRemotePanLength("6500079999999999999", 16)
    }

    @Test
    fun `accountRangeResultListener is called when account ranges are updated`() = runTest {
        val fakeListener = FakeAccountRangeResultListener()

        val service = DefaultCardAccountRangeService(
            cardAccountRangeRepository = createDefaultCardAccountRangeRepository(),
            uiContext = testDispatcher,
            workContext = testDispatcher,
            staticCardAccountRanges = DefaultStaticCardAccountRanges(),
            cardFundingFilter = DefaultCardFundingFilter,
            accountRangeResultListener = fakeListener,
        )

        val expectedAccountRange1 = defaultAccountRange()
        val expectedAccountRange2 = defaultAccountRange(
            lowBinRange = "4000002500001001",
            highBinRange = "4000002500001001",
            brandInfo = AccountRange.BrandInfo.CartesBancaires
        )
        val expectedAccountRange3 = defaultAccountRange(
            lowBinRange = "2221000000000000",
            highBinRange = "2720999999999999",
            brandInfo = AccountRange.BrandInfo.Mastercard,
        )

        service.onCardNumberChanged(CardNumber.Unvalidated("4"), isCbcEligible = false)
        with(fakeListener.awaitItem()) {
            assertThat(accountRanges).containsExactly(expectedAccountRange1, expectedAccountRange2)
            assertThat(unfilteredAccountRanges).containsExactly(expectedAccountRange1, expectedAccountRange2)
        }
        fakeListener.ensureAllEventsConsumed()

        service.onCardNumberChanged(CardNumber.Unvalidated("2"), isCbcEligible = false)
        with(fakeListener.awaitItem()) {
            assertThat(accountRanges).containsExactly(expectedAccountRange3)
            assertThat(unfilteredAccountRanges).containsExactly(expectedAccountRange3)
        }
        fakeListener.ensureAllEventsConsumed()
    }

    @Test
    fun `test the card metadata service is called when funding filter is restrictive`() = runTest {
        // Disallow credit cards
        val restrictiveFundingFilter = FakeCardFundingFilter(disallowedFundingTypes = setOf(CardFunding.Credit))

        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource(isLoading = false)

        val service = DefaultCardAccountRangeService(
            createRemoteDefaultCardAccountRangeRepository(fakeRemoteCardAccountRangeSource),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            cardFundingFilter = restrictiveFundingFilter
        )

        val cardNumber = CardNumber.Unvalidated("4242424242424242")
        service.onCardNumberChanged(cardNumber, isCbcEligible = false)

        assertThat(fakeRemoteCardAccountRangeSource.calls.awaitItem()).isEqualTo(cardNumber)
        fakeRemoteCardAccountRangeSource.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `test the card metadata service is not called when funding filter is default`() = runTest {
        val defaultFundingFilter = DefaultCardFundingFilter

        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource(isLoading = false)

        val service = DefaultCardAccountRangeService(
            createRemoteDefaultCardAccountRangeRepository(fakeRemoteCardAccountRangeSource),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            cardFundingFilter = defaultFundingFilter
        )

        val cardNumber = CardNumber.Unvalidated("4242424242424242")
        service.onCardNumberChanged(cardNumber, isCbcEligible = false)

        fakeRemoteCardAccountRangeSource.calls.expectNoEvents()
    }

    private fun createRemoteDefaultCardAccountRangeRepository(
        remoteCardAccountRangeSource: CardAccountRangeSource
    ): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(applicationContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = remoteCardAccountRangeSource,
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
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

    private suspend fun verifyRemotePanLength(cardNumberString: String, expectedPanLength: Int) {
        val serviceMockRemote = DefaultCardAccountRangeService(
            createDefaultCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            cardFundingFilter = DefaultCardFundingFilter
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        serviceMockRemote.onCardNumberChanged(cardNumber, isCbcEligible = false)

        serviceMockRemote.accountRangeResultFlow.test {
            val newAccountRange = awaitItem().accountRanges.firstOrNull()
            val panLength = newAccountRange?.panLength
            assertThat(panLength).isEqualTo(expectedPanLength)
        }
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
            StripeApiRepository(applicationContext, { publishableKey }, RequestSurface.PaymentElement),
            ApiRequest.Options(publishableKey),
            DefaultCardAccountRangeStore(applicationContext),
            DefaultAnalyticsRequestExecutor(),
            PaymentAnalyticsRequestFactory(applicationContext, publishableKey)
        )
    }

    companion object {
        private fun defaultAccountRange(
            lowBinRange: String = "4000000000000000",
            highBinRange: String = "4999999999999999",
            brandInfo: AccountRange.BrandInfo = AccountRange.BrandInfo.Visa,
            funding: CardFunding = CardFunding.Unknown
        ): AccountRange {
            return AccountRange(
                binRange = BinRange(
                    low = lowBinRange,
                    high = highBinRange,
                ),
                panLength = 16,
                brandInfo = brandInfo,
                funding = funding,
            )
        }
    }
}

@Parcelize
private class FakeCardBrandFilter(
    private val disallowedBrands: Set<CardBrand>
) : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return !disallowedBrands.contains(cardBrand)
    }

    override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
        throw IllegalStateException("Should not be called!")
    }
}

private class FakeAccountRangeResultListener : CardAccountRangeService.AccountRangeResultListener {
    private val calls = Turbine<Call>()

    override fun onAccountRangesResult(accountRanges: List<AccountRange>, unfilteredAccountRanges: List<AccountRange>) {
        calls.add(
            item = Call(
                accountRanges = accountRanges,
                unfilteredAccountRanges = unfilteredAccountRanges
            )
        )
    }

    suspend fun awaitItem() = calls.awaitItem()

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

    data class Call(
        val accountRanges: List<AccountRange>,
        val unfilteredAccountRanges: List<AccountRange>
    )
}

private class FakeCardAccountRangeSource(
    isLoading: Boolean = false
) : CardAccountRangeSource {
    val calls = Turbine<CardNumber.Unvalidated>()

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? {
        calls.add(cardNumber)
        return null
    }

    override val loading: StateFlow<Boolean> = stateFlowOf(isLoading)
}

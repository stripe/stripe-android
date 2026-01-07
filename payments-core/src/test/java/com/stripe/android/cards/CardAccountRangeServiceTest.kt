package com.stripe.android.cards

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
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
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.RequestSurface
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.testing.FakeCardFundingFilter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.TestUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CardAccountRangeServiceTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val applicationContext =
        ApplicationProvider.getApplicationContext<Application>().applicationContext
    private val publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY

    companion object {
        private fun createVisaAccountRange(funding: CardFunding = CardFunding.Credit) = AccountRange(
            binRange = BinRange(
                low = "4000000000000000",
                high = "4999999999999999",
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            funding = funding,
        )

        private fun createMastercardAccountRange(funding: CardFunding = CardFunding.Debit) = AccountRange(
            binRange = BinRange(
                low = "2221000000000000",
                high = "2720999999999999",
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Mastercard,
            funding = funding,
        )

        private fun createAccountRange(
            low: String,
            high: String,
            panLength: Int,
            brandInfo: AccountRange.BrandInfo,
            funding: CardFunding
        ) = AccountRange(
            binRange = BinRange(low, high),
            panLength = panLength,
            brandInfo = brandInfo,
            funding = funding,
        )
    }

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
        cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
    ) {
        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource()

        val service = CardAccountRangeService(
            createCardAccountRangeRepository(fakeRemoteCardAccountRangeSource),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(
                    accountRanges: List<AccountRange>,
                    unfilteredAccountRanges: List<AccountRange>
                ) {
                }
            },
            isCbcEligible = { isCbcEligible },
            cardFundingFilter = cardFundingFilter,
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        service.onCardNumberChanged(cardNumber)

        if (expectedRemoteCall) {
            val calledWith = fakeRemoteCardAccountRangeSource.getAccountRangesCall()
            assertThat(calledWith).isEqualTo(cardNumber)
        }
        fakeRemoteCardAccountRangeSource.ensureAllEventsConsumed()
    }

    @Test
    fun `If CBC is disabled and only one brand matches, return the matched brand as soon as there is input`() =
        runTest {
            val expectedAccountRange = createMastercardAccountRange(CardFunding.Unknown)

            val (accountRanges, _) = testBehavior(
                cardNumber = "2",
                isCbcEligible = false,
            )

            assertThat(accountRanges).containsExactly(expectedAccountRange)
        }

    @Test
    fun `If CBC is enabled, don't return a matched brand until 8 characters are entered`() = runTest {
        val (accountRanges, _) = testBehavior(
            cardNumber = "4",
            isCbcEligible = true,
        )

        assertThat(accountRanges).isEmpty()
    }

    @Test
    fun `If CBC is enabled, return the matched brands once 8 characters are entered`() = runTest {
        val expectedAccountRanges = listOf(
            createAccountRange(
                low = "4000000000000000",
                high = "4999999999999999",
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
                funding = CardFunding.Unknown,
            ),
            createAccountRange(
                low = "4000000000000000",
                high = "4999999999999999",
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.CartesBancaires,
                funding = CardFunding.Unknown,
            )
        )

        val (accountRanges, _) = testBehavior(
            cardNumber = "4000 0000",
            isCbcEligible = true,
            remoteCardAccountRangeSource = FakeCardAccountRangeSource(expectedAccountRanges)
        )

        assertThat(accountRanges).containsExactlyElementsIn(expectedAccountRanges)
    }

    private suspend fun testBehavior(
        cardNumber: String,
        isCbcEligible: Boolean,
        remoteCardAccountRangeSource: CardAccountRangeSource? = null,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter
    ): Pair<List<AccountRange>, List<AccountRange>> {
        val completable = CompletableDeferred<Pair<List<AccountRange>, List<AccountRange>>>()

        val repository = remoteCardAccountRangeSource?.let {
            createCardAccountRangeRepository(it)
        } ?: createDefaultCardAccountRangeRepository()

        val service = CardAccountRangeService(
            cardAccountRangeRepository = repository,
            uiContext = testDispatcher,
            workContext = testDispatcher,
            staticCardAccountRanges = DefaultStaticCardAccountRanges(),
            accountRangeResultListener = object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(
                    accountRanges: List<AccountRange>,
                    unfilteredAccountRanges: List<AccountRange>
                ) {
                    completable.complete(Pair(accountRanges, unfilteredAccountRanges))
                }
            },
            isCbcEligible = { isCbcEligible },
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
        )

        service.onCardNumberChanged(CardNumber.Unvalidated(cardNumber))

        return completable.await()
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
            val (filteredAccountRanges, unfilteredAccountRanges) = testBehavior(
                cardNumber = cardNumber,
                isCbcEligible = false,
                cardBrandFilter = cardBrandFilter
            )

            // Since Mastercard is disallowed, the accountRanges should be empty
            assertThat(filteredAccountRanges).isEmpty()
            // Even though mastercard is disallowed, it should be in the unfiltered ranges
            assertEquals(unfilteredAccountRanges.count(), 1)
            assertEquals(unfilteredAccountRanges.firstOrNull()?.brand, CardBrand.MasterCard)
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
            val (filteredAccountRanges, _) = testBehavior(
                cardNumber = cardNumber,
                isCbcEligible = false,
                cardBrandFilter = cardBrandFilter
            )

            // Since Mastercard is allowed, the accountRanges should have contents
            assertThat(filteredAccountRanges).isNotEmpty()
        }

    private fun createCardAccountRangeRepository(
        remoteSource: CardAccountRangeSource
    ): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(applicationContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = remoteSource,
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
        val service = CardAccountRangeService(
            createDefaultCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            DefaultStaticCardAccountRanges(),
            object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(
                    accountRanges: List<AccountRange>,
                    unfilteredAccountRanges: List<AccountRange>
                ) {
                    val newAccountRange = accountRanges.firstOrNull()
                    panLength = newAccountRange?.panLength
                    latch.countDown()
                }
            },
            isCbcEligible = { false },
            cardFundingFilter = DefaultCardFundingFilter,
        )

        val cardNumber = CardNumber.Unvalidated(cardNumberString)
        service.onCardNumberChanged(cardNumber)
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
            StripeApiRepository(applicationContext, { publishableKey }, RequestSurface.PaymentElement),
            ApiRequest.Options(publishableKey),
            DefaultCardAccountRangeStore(applicationContext),
            DefaultAnalyticsRequestExecutor(),
            PaymentAnalyticsRequestFactory(applicationContext, publishableKey)
        )
    }

    @Test
    fun `When funding filter rejects any types, remote call is made when cbc ineligible`() = runTest {
        val fundingFilter = FakeCardFundingFilter(disallowedFundingTypes = setOf(CardFunding.Debit))

        val visaAccountRange = ACCOUNTS.first { it.brand == CardBrand.Visa }

        testIfRemoteCalled(
            isCbcEligible = false,
            cardNumberString = visaAccountRange.binRange.low,
            expectedRemoteCall = true,
            cardFundingFilter = fundingFilter
        )
    }

    @Test
    fun `When funding filter accepts all types, remote call is made when cbc eligible`() = runTest {
        val visaAccountRange = ACCOUNTS.first { it.brand == CardBrand.Visa }

        testIfRemoteCalled(
            isCbcEligible = true,
            cardNumberString = visaAccountRange.binRange.low,
            expectedRemoteCall = true,
            cardFundingFilter = DefaultCardFundingFilter
        )
    }

    private fun createServiceWithRemoteSource(
        remoteCardAccountRangeSource: CardAccountRangeSource,
        isCbcEligible: Boolean = true
    ): CardAccountRangeService {
        return CardAccountRangeService(
            cardAccountRangeRepository = createCardAccountRangeRepository(
                remoteCardAccountRangeSource
            ),
            uiContext = testDispatcher,
            workContext = testDispatcher,
            staticCardAccountRanges = DefaultStaticCardAccountRanges(),
            accountRangeResultListener = object : CardAccountRangeService.AccountRangeResultListener {
                override fun onAccountRangesResult(
                    accountRanges: List<AccountRange>,
                    unfilteredAccountRanges: List<AccountRange>
                ) = Unit
            },
            isCbcEligible = { isCbcEligible },
            cardFundingFilter = DefaultCardFundingFilter,
        )
    }

    @Test
    fun `accountRangesStateFlow updates when account ranges change`() = runTest {
        val expectedAccountRange = createVisaAccountRange()

        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource(listOf(expectedAccountRange))

        val service = createServiceWithRemoteSource(fakeRemoteCardAccountRangeSource)

        service.accountRangesStateFlow.test {
            assertThat(awaitItem()).isEmpty()

            service.onCardNumberChanged(CardNumber.Unvalidated("4242424242424242"))

            assertThat(awaitItem()).containsExactly(expectedAccountRange)
        }
    }

    @Test
    fun `accountRangeFlow property updates when accountRangesStateFlow changes`() = runTest {
        val expectedAccountRange = createVisaAccountRange()

        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource(listOf(expectedAccountRange))

        val service = createServiceWithRemoteSource(fakeRemoteCardAccountRangeSource)

        service.accountRangeFlow.test {
            assertThat(awaitItem()).isNull()

            service.onCardNumberChanged(CardNumber.Unvalidated("4242424242424242"))

            assertThat(awaitItem()).isEqualTo(expectedAccountRange)
        }
    }

    @Test
    fun `accountRange property updates when accountRangesStateFlow changes`() = runTest {
        val accountRange1 = createVisaAccountRange()
        val accountRange2 = createMastercardAccountRange()

        val fakeRemoteCardAccountRangeSource = FakeCardAccountRangeSource()

        val service = createServiceWithRemoteSource(fakeRemoteCardAccountRangeSource)

        assertThat(service.accountRange).isNull()

        fakeRemoteCardAccountRangeSource.accountRangesToReturn = listOf(accountRange1)
        service.onCardNumberChanged(CardNumber.Unvalidated("4242424242424242"))
        assertThat(service.accountRange).isEqualTo(accountRange1)

        fakeRemoteCardAccountRangeSource.accountRangesToReturn = listOf(accountRange2)
        service.onCardNumberChanged(CardNumber.Unvalidated("2221000000000000"))
        assertThat(service.accountRange).isEqualTo(accountRange2)
    }
}

private class FakeCardAccountRangeSource(
    accountRangesToReturn: List<AccountRange>? = null
) : CardAccountRangeSource {
    private val calls = Turbine<CardNumber.Unvalidated>()
    var accountRangesToReturn: List<AccountRange>? = accountRangesToReturn

    override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
        calls.add(cardNumber)
        return accountRangesToReturn
    }

    override val loading: StateFlow<Boolean> = stateFlowOf(false)

    suspend fun getAccountRangesCall() = calls.awaitItem()

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()
}

@Parcelize
private class FakeCardBrandFilter(
    private val disallowedBrands: Set<CardBrand>
) : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return !disallowedBrands.contains(cardBrand)
    }
}

package com.stripe.android.ui.core.elements

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardAccountRangeService.AccountRangesState
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ControllerAccountRangeServiceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `onCardNumberChanged calls both default and funding services`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val fundingService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            defaultService = defaultService,
            fundingService = fundingService
        )

        val cardNumber = CardNumber.Unvalidated("4242424242424242")
        controllerService.onCardNumberChanged(cardNumber, isCbcEligible = false)

        val defaultCall = defaultService.onCardNumberChangedTurbine.awaitItem()
        assertThat(defaultCall.first).isEqualTo(cardNumber)
        assertThat(defaultCall.second).isFalse()

        val fundingCall = fundingService.onCardNumberChangedTurbine.awaitItem()
        assertThat(fundingCall.first).isEqualTo(cardNumber)
        assertThat(fundingCall.second).isFalse()

        defaultService.onCardNumberChangedTurbine.ensureAllEventsConsumed()
        fundingService.onCardNumberChangedTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `onCardNumberChanged passes isCbcEligible to both services`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val fundingService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            defaultService = defaultService,
            fundingService = fundingService
        )

        val cardNumber = CardNumber.Unvalidated("4000056655665556")
        controllerService.onCardNumberChanged(cardNumber, isCbcEligible = true)

        val defaultCall = defaultService.onCardNumberChangedTurbine.awaitItem()
        assertThat(defaultCall.second).isTrue()

        val fundingCall = fundingService.onCardNumberChangedTurbine.awaitItem()
        assertThat(fundingCall.second).isFalse()
    }

    @Test
    fun `fundingAccountRanges emits ranges from funding service`() = runTest {
        val fundingService = FakeCardAccountRangeService()
        val visaRange = createAccountRange(CardBrand.Visa)

        val controllerService = createControllerService(
            fundingService = fundingService
        )

        controllerService.fundingAccountRanges.test {
            assertThat(awaitItem()).isEmpty()

            fundingService.emitAccountRanges(listOf(visaRange))

            assertThat(awaitItem()).containsExactly(visaRange)
        }
    }

    @Test
    fun `fundingAccountRanges filters out Loading state`() = runTest {
        val fundingService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            fundingService = fundingService
        )

        controllerService.fundingAccountRanges.test {
            assertThat(awaitItem()).isEmpty()

            fundingService.emitLoading()

            // Should not emit for Loading state
            expectNoEvents()

            val visaRange = createAccountRange(CardBrand.Visa)
            fundingService.emitAccountRanges(listOf(visaRange))

            assertThat(awaitItem()).containsExactly(visaRange)
        }
    }

    @Test
    fun `isLoading delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        controllerService.isLoading.test {
            assertThat(awaitItem()).isFalse()

            defaultService.setLoading(true)
            assertThat(awaitItem()).isTrue()

            defaultService.setLoading(false)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `accountRangesStateFlow delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val visaRange = createAccountRange(CardBrand.Visa)

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        controllerService.accountRangesStateFlow.test {
            val initialState = awaitItem()
            assertThat(initialState).isInstanceOf(AccountRangesState.Success::class.java)
            assertThat(initialState.ranges).isEmpty()

            defaultService.emitAccountRanges(listOf(visaRange))

            val updatedState = awaitItem()
            assertThat(updatedState).isInstanceOf(AccountRangesState.Success::class.java)
            assertThat(updatedState.ranges).containsExactly(visaRange)
        }
    }

    @Test
    fun `accountRange delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val visaRange = createAccountRange(CardBrand.Visa)

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        assertThat(controllerService.accountRange).isNull()

        defaultService.emitAccountRanges(listOf(visaRange))

        assertThat(controllerService.accountRange).isEqualTo(visaRange)
    }

    @Test
    fun `accountRangeResultFlow delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val visaRange = createAccountRange(CardBrand.Visa)

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        controllerService.accountRangeResultFlow.test {
            val initialResult = awaitItem()
            assertThat(initialResult.accountRanges).isEmpty()

            defaultService.emitAccountRanges(listOf(visaRange))

            val updatedResult = awaitItem()
            assertThat(updatedResult.accountRanges).containsExactly(visaRange)
        }
    }

    @Test
    fun `queryAccountRangeRepository delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        val cardNumber = CardNumber.Unvalidated("4242424242424242")
        controllerService.queryAccountRangeRepository(cardNumber)

        val call = defaultService.queryAccountRangeRepositoryTurbine.awaitItem()
        assertThat(call).isEqualTo(cardNumber)
    }

    @Test
    fun `cancelAccountRangeRepositoryJob delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        controllerService.cancelAccountRangeRepositoryJob()

        defaultService.cancelAccountRangeRepositoryJobTurbine.awaitItem()
    }

    @Test
    fun `updateAccountRangesResult delegates to default service`() = runTest {
        val defaultService = FakeCardAccountRangeService()
        val visaRange = createAccountRange(CardBrand.Visa)

        val controllerService = createControllerService(
            defaultService = defaultService
        )

        controllerService.updateAccountRangesResult(listOf(visaRange))

        val call = defaultService.updateAccountRangesResultTurbine.awaitItem()
        assertThat(call).containsExactly(visaRange)
    }

    private fun createControllerService(
        defaultService: FakeCardAccountRangeService = FakeCardAccountRangeService(),
        fundingService: FakeCardAccountRangeService = FakeCardAccountRangeService()
    ): ControllerAccountRangeService {
        val defaultFactory = object : CardAccountRangeService.Factory {
            override fun create(
                cardBrandFilter: CardBrandFilter,
                cardFundingFilter: CardFundingFilter,
                accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
            ): CardAccountRangeService = defaultService
        }

        val fundingFactory = object : CardAccountRangeService.Factory {
            override fun create(
                cardBrandFilter: CardBrandFilter,
                cardFundingFilter: CardFundingFilter,
                accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
            ): CardAccountRangeService = fundingService
        }

        return ControllerAccountRangeService(
            cardAccountRangeServiceFactory = defaultFactory,
            fundingCardAccountRangeServiceFactory = fundingFactory,
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            coroutineScope = testScope
        )
    }

    private fun createAccountRange(
        brand: CardBrand,
        panLength: Int = 16
    ): AccountRange {
        val brandInfo = when (brand) {
            CardBrand.Visa -> AccountRange.BrandInfo.Visa
            CardBrand.MasterCard -> AccountRange.BrandInfo.Mastercard
            CardBrand.AmericanExpress -> AccountRange.BrandInfo.AmericanExpress
            CardBrand.Discover -> AccountRange.BrandInfo.Discover
            CardBrand.JCB -> AccountRange.BrandInfo.JCB
            CardBrand.DinersClub -> AccountRange.BrandInfo.DinersClub
            CardBrand.UnionPay -> AccountRange.BrandInfo.UnionPay
            CardBrand.CartesBancaires -> AccountRange.BrandInfo.CartesBancaires
            else -> AccountRange.BrandInfo.Visa
        }
        return AccountRange(
            binRange = BinRange(low = "4242420000000000", high = "4242424239999999"),
            panLength = panLength,
            brandInfo = brandInfo,
            funding = CardFunding.Credit,
            country = "US"
        )
    }

    private class FakeCardAccountRangeService : CardAccountRangeService {
        private val _isLoading = MutableStateFlow(false)
        override val isLoading: StateFlow<Boolean> = _isLoading

        private val _accountRangesStateFlow = MutableStateFlow<AccountRangesState>(
            AccountRangesState.Success(emptyList(), emptyList())
        )
        override val accountRangesStateFlow: StateFlow<AccountRangesState> = _accountRangesStateFlow

        override val accountRangeResultFlow: Flow<CardAccountRangeService.AccountRangesResult> =
            _accountRangesStateFlow
                .filterIsInstance<AccountRangesState.Success>()
                .map { state ->
                    CardAccountRangeService.AccountRangesResult(
                        accountRanges = state.ranges,
                        unfilteredAccountRanges = state.unfilteredRanges
                    )
                }

        private val _onCardNumberChangedTurbine = Turbine<Pair<CardNumber.Unvalidated, Boolean>>()
        val onCardNumberChangedTurbine: ReceiveTurbine<Pair<CardNumber.Unvalidated, Boolean>> =
            _onCardNumberChangedTurbine

        private val _queryAccountRangeRepositoryTurbine = Turbine<CardNumber.Unvalidated>()
        val queryAccountRangeRepositoryTurbine: ReceiveTurbine<CardNumber.Unvalidated> =
            _queryAccountRangeRepositoryTurbine

        private val _updateAccountRangesResultTurbine = Turbine<List<AccountRange>>()
        val updateAccountRangesResultTurbine: ReceiveTurbine<List<AccountRange>> =
            _updateAccountRangesResultTurbine

        private val _cancelAccountRangeRepositoryJobTurbine = Turbine<Unit>()
        val cancelAccountRangeRepositoryJobTurbine: ReceiveTurbine<Unit> =
            _cancelAccountRangeRepositoryJobTurbine

        override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated, isCbcEligible: Boolean) {
            _onCardNumberChangedTurbine.add(cardNumber to isCbcEligible)
        }

        override fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
            _queryAccountRangeRepositoryTurbine.add(cardNumber)
        }

        override fun cancelAccountRangeRepositoryJob() {
            _cancelAccountRangeRepositoryJobTurbine.add(Unit)
        }

        override fun updateAccountRangesResult(accountRanges: List<AccountRange>) {
            _updateAccountRangesResultTurbine.add(accountRanges)
            emitAccountRanges(accountRanges)
        }

        fun setLoading(loading: Boolean) {
            _isLoading.value = loading
        }

        fun emitLoading() {
            _accountRangesStateFlow.value = AccountRangesState.Loading
        }

        fun emitAccountRanges(ranges: List<AccountRange>) {
            _accountRangesStateFlow.value = AccountRangesState.Success(ranges, ranges)
        }
    }
}

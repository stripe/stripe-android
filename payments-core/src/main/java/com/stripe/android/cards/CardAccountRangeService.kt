package com.stripe.android.cards

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val MIN_CARD_NUMBER_LENGTH = 8

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardAccountRangeService {

    val isLoading: StateFlow<Boolean>
    val accountRangeResultFlow: Flow<AccountRangesResult>
    val accountRangesStateFlow: StateFlow<List<AccountRange>>

    val accountRange: AccountRange?
        get() = accountRangesStateFlow.value.firstOrNull()

    fun onCardNumberChanged(
        cardNumber: CardNumber.Unvalidated,
        isCbcEligible: () -> Boolean
    )

    @JvmSynthetic
    fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated)

    fun cancelAccountRangeRepositoryJob()

    fun updateAccountRangesResult(accountRanges: List<AccountRange>)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface AccountRangesResult {
        val accountRanges: List<AccountRange>
        val unfilteredAccountRanges: List<AccountRange>

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(
            override val accountRanges: List<AccountRange>,
            override val unfilteredAccountRanges: List<AccountRange>
        ) : AccountRangesResult

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Loading : AccountRangesResult {
            override val accountRanges: List<AccountRange> = emptyList()
            override val unfilteredAccountRanges: List<AccountRange> = emptyList()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface AccountRangeResultListener {
        fun onAccountRangesResult(accountRanges: List<AccountRange>, unfilteredAccountRanges: List<AccountRange>)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeService(
    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val uiContext: CoroutineContext,
    private val workContext: CoroutineContext,
    val staticCardAccountRanges: StaticCardAccountRanges,
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    private val accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(uiContext)
) : CardAccountRangeService {

    override val isLoading: StateFlow<Boolean> = cardAccountRangeRepository.loading
    private var lastBin: Bin? = null

    private val _accountRangeResultFlow = MutableSharedFlow<CardAccountRangeService.AccountRangesResult>(replay = 1)
    override val accountRangeResultFlow = _accountRangeResultFlow

    override val accountRangesStateFlow: StateFlow<List<AccountRange>> = _accountRangeResultFlow
        .map { it.accountRanges }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    @VisibleForTesting
    var accountRangeRepositoryJob: Job? = null

    override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated, isCbcEligible: () -> Boolean) {
        val isCbcEligible = isCbcEligible()
        val shouldQuery = !isCbcEligible || cardNumber.length >= MIN_CARD_NUMBER_LENGTH
        if (!shouldQuery) {
            updateAccountRangesResult(emptyList())
            return
        }

        val testAccountRanges = if (isCbcEligible()) {
            CbcTestCardDelegate.onCardNumberChanged(cardNumber)
        } else {
            emptyList()
        }

        if (testAccountRanges.isNotEmpty()) {
            updateAccountRangesResult(testAccountRanges)
            return
        }

        val staticAccountRanges = staticCardAccountRanges.filter(cardNumber)

        if (isCbcEligible) {
            queryAccountRangeRepository(cardNumber)
        } else {
            if (staticAccountRanges.isEmpty() || shouldQueryRepository(staticAccountRanges)) {
                // query for AccountRange data
                queryAccountRangeRepository(cardNumber)
            } else {
                // use static AccountRange data
                updateAccountRangesResult(staticAccountRanges)
            }
        }
    }

    @JvmSynthetic
    override fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
        if (shouldQueryAccountRange(cardNumber)) {
            // cancel in-flight job
            cancelAccountRangeRepositoryJob()

            // Emit loading state before fetching
            _accountRangeResultFlow.tryEmit(CardAccountRangeService.AccountRangesResult.Loading)

            accountRangeRepositoryJob = coroutineScope.launch(workContext) {
                val bin = cardNumber.bin

                val accountRanges = if (bin != null) {
                    cardAccountRangeRepository.getAccountRanges(cardNumber)
                } else {
                    null
                }

                withContext(uiContext) {
                    updateAccountRangesResult(accountRanges.orEmpty())
                }
            }
        }
    }

    override fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    override fun updateAccountRangesResult(accountRanges: List<AccountRange>) {
        val filteredAccountRanges = accountRanges.filter { cardBrandFilter.isAccepted(it.brand) }
        _accountRangeResultFlow.tryEmit(
            value = CardAccountRangeService.AccountRangesResult.Success(
                accountRanges = filteredAccountRanges,
                unfilteredAccountRanges = accountRanges
            )
        )
        accountRangeResultListener?.onAccountRangesResult(
            accountRanges = filteredAccountRanges,
            unfilteredAccountRanges = accountRanges
        )
    }

    private fun shouldQueryRepository(
        accountRanges: List<AccountRange>
    ) = when (accountRanges.firstOrNull()?.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        val accountRange = accountRangesStateFlow.value.firstOrNull()
        val shouldQuery = accountRange == null ||
            cardNumber.bin == null ||
            !accountRange.binRange.matches(cardNumber) ||
            cardNumber.bin != lastBin
        lastBin = cardNumber.bin
        return shouldQuery
    }
}

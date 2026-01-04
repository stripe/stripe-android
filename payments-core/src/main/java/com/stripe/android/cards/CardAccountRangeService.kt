package com.stripe.android.cards

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val MIN_CARD_NUMBER_LENGTH = 8

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardAccountRangeService(
    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val uiContext: CoroutineContext,
    private val workContext: CoroutineContext,
    val staticCardAccountRanges: StaticCardAccountRanges,
    private val accountRangeResultListener: AccountRangeResultListener,
    private val isCbcEligible: () -> Boolean,
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    private val cardFundingFilter: CardFundingFilter
) {

    private val needsRemoteFundingType = needsFundingType()
    val isLoading: StateFlow<Boolean> = cardAccountRangeRepository.loading
    private var lastBin: Bin? = null

    private val _accountRangesStateFlow = MutableStateFlow(emptyList<AccountRange>())
    val accountRangesStateFlow: StateFlow<List<AccountRange>> = _accountRangesStateFlow

    val accountRangeFlow = accountRangesStateFlow.map {
        it.firstOrNull()
    }.stateIn(
        scope = CoroutineScope(uiContext),
        started = SharingStarted.Lazily,
        initialValue = accountRangesStateFlow.value.firstOrNull()
    )

    val accountRange: AccountRange?
        get() = accountRangesStateFlow.value.firstOrNull()

    @VisibleForTesting
    var accountRangeRepositoryJob: Job? = null

    fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated) {
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

        if (isCbcEligible || needsRemoteFundingType) {
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
    fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
        if (shouldQueryAccountRange(cardNumber)) {
            // cancel in-flight job
            cancelAccountRangeRepositoryJob()

            // invalidate accountRange before fetching
            _accountRangesStateFlow.value = emptyList()

            accountRangeRepositoryJob = CoroutineScope(workContext).launch {
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

    fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    fun updateAccountRangesResult(accountRanges: List<AccountRange>) {
        _accountRangesStateFlow.value = accountRanges.filter { cardBrandFilter.isAccepted(it.brand) }
        accountRangeResultListener.onAccountRangesResult(accountRangesStateFlow.value, accountRanges)
    }

    private fun shouldQueryRepository(
        accountRanges: List<AccountRange>
    ) = when (accountRanges.firstOrNull()?.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        val shouldQuery = accountRange == null ||
            cardNumber.bin == null ||
            accountRange?.binRange?.matches(cardNumber) == false ||
            cardNumber.bin != lastBin
        lastBin = cardNumber.bin
        return shouldQuery
    }

    private fun needsFundingType(): Boolean {
        return CardFunding.entries.any { cardFundingFilter.isAccepted(it).not() }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface AccountRangeResultListener {
        fun onAccountRangesResult(accountRanges: List<AccountRange>, unfilteredAccountRanges: List<AccountRange>)
    }
}

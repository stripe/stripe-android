package com.stripe.android.cards

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter
) {

    private val needsRemoteQueryForFunding = CardFunding.entries.any {
        cardFundingFilter.isAccepted(it).not()
    }

    val isLoading: StateFlow<Boolean> = cardAccountRangeRepository.loading
    private var lastBin: Bin? = null

    private val _accountRangeState = MutableStateFlow<AccountRangeState>(
        value = AccountRangeState.Success(emptyList())
    )

    val accountRangeState: StateFlow<AccountRangeState> = _accountRangeState

    val accountRange: AccountRange?
        get() = accountRangeState.value.ranges.firstOrNull()

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

        if (isCbcEligible || needsRemoteQueryForFunding) {
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
            _accountRangeState.value = AccountRangeState.Loading

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
        val filteredAccountRanges = accountRanges.filter { cardBrandFilter.isAccepted(it.brand) }
        this._accountRangeState.value = AccountRangeState.Success(filteredAccountRanges)
        accountRangeResultListener.onAccountRangesResult(filteredAccountRanges, accountRanges)
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface AccountRangeResultListener {
        fun onAccountRangesResult(accountRanges: List<AccountRange>, unfilteredAccountRanges: List<AccountRange>)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface AccountRangeState {
        val ranges: List<AccountRange>

        data object Loading : AccountRangeState {
            override val ranges = emptyList<AccountRange>()
        }

        data class Success(override val ranges: List<AccountRange>) : AccountRangeState
    }
}

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val MIN_CARD_NUMBER_LENGTH = 8

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardAccountRangeService {

    val isLoading: StateFlow<Boolean>
    val accountRangeResultFlow: Flow<AccountRangesResult>
    val accountRangesStateFlow: StateFlow<AccountRangesState>

    val accountRange: AccountRange?
        get() = accountRangesStateFlow.value.ranges.firstOrNull()

    fun onCardNumberChanged(
        cardNumber: CardNumber.Unvalidated,
        isCbcEligible: Boolean
    )

    @JvmSynthetic
    fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated)

    fun cancelAccountRangeRepositoryJob()

    fun updateAccountRangesResult(accountRanges: List<AccountRange>)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class AccountRangesResult(
        val accountRanges: List<AccountRange>,
        val unfilteredAccountRanges: List<AccountRange>
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface AccountRangeResultListener {
        fun onAccountRangesResult(accountRanges: List<AccountRange>, unfilteredAccountRanges: List<AccountRange>)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface AccountRangesState {
        val ranges: List<AccountRange>
        val unfilteredRanges: List<AccountRange>

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Loading : AccountRangesState {
            override val ranges: List<AccountRange> = emptyList()
            override val unfilteredRanges: List<AccountRange> = emptyList()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(
            override val ranges: List<AccountRange>,
            override val unfilteredRanges: List<AccountRange>
        ) : AccountRangesState
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeService(
    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val uiContext: CoroutineContext,
    private val workContext: CoroutineContext,
    val staticCardAccountRanges: StaticCardAccountRanges,
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    private val cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
    private val accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(uiContext + SupervisorJob())
) : CardAccountRangeService {

    private val needsRemoteQueryForFunding = CardFunding.entries.any {
        cardFundingFilter.isAccepted(it).not()
    }

    override val isLoading: StateFlow<Boolean> = cardAccountRangeRepository.loading
    private var repositoryResult: RepositoryResult? = null

    private val _accountRangesStateFlow = MutableStateFlow<CardAccountRangeService.AccountRangesState>(
        value = CardAccountRangeService.AccountRangesState.Success(emptyList(), emptyList())
    )

    override val accountRangesStateFlow: StateFlow<CardAccountRangeService.AccountRangesState> =
        _accountRangesStateFlow

    override val accountRangeResultFlow: Flow<CardAccountRangeService.AccountRangesResult> =
        accountRangesStateFlow
            .filterIsInstance<CardAccountRangeService.AccountRangesState.Success>()
            .map { state ->
                CardAccountRangeService.AccountRangesResult(
                    accountRanges = state.ranges,
                    unfilteredAccountRanges = state.unfilteredRanges
                )
            }

    @VisibleForTesting
    var accountRangeRepositoryJob: Job? = null

    override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated, isCbcEligible: Boolean) {
        val shouldQuery = !isCbcEligible || cardNumber.length >= MIN_CARD_NUMBER_LENGTH
        if (!shouldQuery) {
            updateAccountRangesResult(emptyList())
            return
        }

        val testAccountRanges = if (isCbcEligible) {
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
    override fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
        // cancel in-flight job
        cancelAccountRangeRepositoryJob()

        val bin = cardNumber.bin
        val cachedResult = repositoryResult?.takeIf { bin != null && it.bin == bin }
        if (cachedResult != null) {
            publishPrioritizedRanges(cachedResult.accountRanges, cardNumber)
            return
        }

        // Emit loading state before fetching
        _accountRangesStateFlow.value = CardAccountRangeService.AccountRangesState.Loading

        accountRangeRepositoryJob = coroutineScope.launch(workContext) {
            val accountRanges = if (bin != null) {
                cardAccountRangeRepository.getAccountRanges(cardNumber)
            } else {
                null
            }.orEmpty()

            withContext(uiContext) {
                repositoryResult = bin?.let { RepositoryResult(it, accountRanges) }
                publishPrioritizedRanges(accountRanges, cardNumber)
            }
        }
    }

    override fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    override fun updateAccountRangesResult(accountRanges: List<AccountRange>) {
        repositoryResult = null
        publishAccountRanges(accountRanges)
    }

    private fun publishAccountRanges(accountRanges: List<AccountRange>) {
        val filteredAccountRanges = accountRanges.filter { cardBrandFilter.isAccepted(it.brand) }

        // Single source update - both filtered and unfiltered
        _accountRangesStateFlow.value = CardAccountRangeService.AccountRangesState.Success(
            ranges = filteredAccountRanges,
            unfilteredRanges = accountRanges
        )

        // Listener callback (maintains backward compatibility)
        accountRangeResultListener?.onAccountRangesResult(
            accountRanges = filteredAccountRanges,
            unfilteredAccountRanges = accountRanges
        )
    }

    /**
     * The repository result depends only on the BIN, but the range that applies depends on the
     * whole card number, so the order is recomputed for every card number within the BIN.
     * Publishing only on a change keeps listeners from reacting to every keystroke.
     */
    private fun publishPrioritizedRanges(
        accountRanges: List<AccountRange>,
        cardNumber: CardNumber.Unvalidated
    ) {
        val prioritizedRanges = prioritizeMatchingRanges(accountRanges, cardNumber)
        val state = accountRangesStateFlow.value

        if (state !is CardAccountRangeService.AccountRangesState.Success ||
            state.unfilteredRanges != prioritizedRanges
        ) {
            publishAccountRanges(prioritizedRanges)
        }
    }

    /**
     * The repository returns every range for the 6-digit BIN, and a BIN can mix PAN lengths across
     * its sub-ranges. Consumers read the first range, so that range must be one that contains
     * [cardNumber].
     *
     * If the BIN has ranges but none contains [cardNumber], we use the static data instead.
     */
    private fun prioritizeMatchingRanges(
        accountRanges: List<AccountRange>,
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange> {
        val (matchingRanges, otherRanges) = accountRanges.partition { it.binRange.matches(cardNumber) }
        val fallbackRanges = if (matchingRanges.isEmpty() && otherRanges.isNotEmpty()) {
            staticCardAccountRanges.filter(cardNumber)
        } else {
            emptyList()
        }

        return matchingRanges + fallbackRanges + otherRanges
    }

    private fun shouldQueryRepository(
        accountRanges: List<AccountRange>
    ) = when (accountRanges.firstOrNull()?.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
    }

    private class RepositoryResult(
        val bin: Bin,
        val accountRanges: List<AccountRange>,
    )
}

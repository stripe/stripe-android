package com.stripe.android.cards

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.toBrands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardAccountRangeService constructor(
    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val workContext: CoroutineContext,
    val staticCardAccountRanges: StaticCardAccountRanges,
    private val accountRangeResultListener: AccountRangeResultListener
) {

    val isLoading: Flow<Boolean> = cardAccountRangeRepository.loading

    var accountRange: AccountRange? = null
        private set

    @VisibleForTesting
    var accountRangeRepositoryJob: Job? = null

    private var possibleBrandsJob: Job? = null

    fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated) {
        val staticAccountRange = staticCardAccountRanges.filter(cardNumber)
            .let { accountRanges ->
                if (accountRanges.size == 1) {
                    accountRanges.first()
                } else {
                    null
                }
            }
        if (staticAccountRange == null || shouldQueryRepository(staticAccountRange)) {
            // query for AccountRange data
            queryAccountRangeRepository(cardNumber)
        } else {
            // use static AccountRange data
            updateAccountRangeResult(staticAccountRange)
        }

        retrievePossibleCardBrands(cardNumber)
    }

    @JvmSynthetic
    fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
        if (shouldQueryAccountRange(cardNumber)) {
            // cancel in-flight job
            cancelAccountRangeRepositoryJob()

            // invalidate accountRange before fetching
            accountRange = null

            accountRangeRepositoryJob = CoroutineScope(workContext).launch {
                val bin = cardNumber.bin
                val accountRange = if (bin != null) {
                    cardAccountRangeRepository.getAccountRange(cardNumber)
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    updateAccountRangeResult(accountRange)
                }
            }
        }
    }

    fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    private fun cancelPossibleBrandsJob() {
        possibleBrandsJob?.cancel()
        possibleBrandsJob = null
    }

    @JvmSynthetic
    fun updateAccountRangeResult(
        newAccountRange: AccountRange?
    ) {
        accountRange = newAccountRange
        accountRangeResultListener.onAccountRangeResult(accountRange)
    }

    private fun retrievePossibleCardBrands(cardNumber: CardNumber.Unvalidated) {
        cancelPossibleBrandsJob()

        accountRangeRepositoryJob = CoroutineScope(workContext).launch {
            val bin = cardNumber.bin

            val accountRanges = buildSet {
                accountRange?.let { add(it) }
                if (bin != null) {
                    cardAccountRangeRepository.getAccountRanges(cardNumber)
                } else {
                    null
                }?.let {
                    addAll(
                        it
                    )
                }
            }

            withContext(Dispatchers.Main) {
                accountRangeResultListener.onPossibleBrands(accountRanges.toBrands())
            }
        }
    }

    private fun shouldQueryRepository(
        accountRange: AccountRange
    ) = when (accountRange.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        return accountRange == null ||
            cardNumber.bin == null ||
            accountRange?.binRange?.matches(cardNumber) == false
    }

    interface AccountRangeResultListener {
        fun onAccountRangeResult(newAccountRange: AccountRange?)

        fun onPossibleBrands(possibleBrands: Set<CardBrand>) { }
    }
}

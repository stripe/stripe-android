package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardAccountRangeService constructor(
    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val workContext: CoroutineContext,
    private val accountRangeResultListener: AccountRangeResultListener
) {

    var accountRange: AccountRange? = null
        private set
    private var accountRangeRepositoryJob: Job? = null

    fun shouldQueryRepository(
        accountRange: AccountRange
    ) = when (accountRange.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
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
                    accountRangeResultListener.onAccountRangeResult(accountRange)
                }
            }
        }
    }

    fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    @JvmSynthetic
    fun updateAccountRangeResult(
        newAccountRange: AccountRange?
    ) {
        accountRange = newAccountRange
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        return accountRange == null ||
            cardNumber.bin == null ||
            accountRange?.binRange?.matches(cardNumber) == false
    }

    interface AccountRangeResultListener {
        fun onAccountRangeResult(newAccountRange: AccountRange?)
    }
}

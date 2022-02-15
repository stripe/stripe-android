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
interface CardAccountRangeService {

    var accountRange: AccountRange?
    val cardAccountRangeRepository: CardAccountRangeRepository
    var accountRangeRepositoryJob: Job?
    val workContext: CoroutineContext

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
                    onAccountRangeResult(accountRange)
                }
            }
        }
    }

    fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    @JvmSynthetic
    fun onAccountRangeResult(
        newAccountRange: AccountRange?
    ) {
        accountRange = newAccountRange
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        return accountRange == null ||
            cardNumber.bin == null ||
            accountRange?.binRange?.matches(cardNumber) == false
    }
}

package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardAccountRangeServiceDelegate {
    fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated): Boolean
    fun onResult(accountRanges: List<AccountRange>?)
    fun onUnregister()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CbcDelegate(
    private var brandsListener: ((List<CardBrand>) -> Unit)?,
) : CardAccountRangeServiceDelegate {

    private val testCards = mapOf(
        "40000025" to listOf(CardBrand.CartesBancaires, CardBrand.Visa),
        "55555525" to listOf(CardBrand.CartesBancaires, CardBrand.MasterCard),
    )

    override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated): Boolean {
        val prefix = cardNumber.normalized.take(8)
        val brands = testCards[prefix].orEmpty()
        brandsListener?.invoke(brands)
        return brands.isNotEmpty()
    }

    override fun onResult(accountRanges: List<AccountRange>?) {
        val brands = accountRanges.orEmpty().map { it.brand }.distinct()
        brandsListener?.invoke(brands)
    }

    override fun onUnregister() {
        brandsListener = null
    }
}

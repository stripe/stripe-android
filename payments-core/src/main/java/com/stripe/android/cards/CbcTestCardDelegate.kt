package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CbcTestCardDelegate {

    private val testAccountRanges = mapOf(
        "4000002500001001" to listOf(
            AccountRange(
                BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.CartesBancaires,
            ),
            AccountRange(
                BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Visa,
            ),
        ),
        "5555552500001001" to listOf(
            AccountRange(
                binRange = BinRange(
                    low = "5100000000000000",
                    high = "5599999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.CartesBancaires,
            ),
            AccountRange(
                binRange = BinRange(
                    low = "5100000000000000",
                    high = "5599999999999999"
                ),
                panLength = 16,
                brandInfo = AccountRange.BrandInfo.Mastercard,
            ),
        ),
    )

    fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated): List<AccountRange> {
        val matches = testAccountRanges.filterKeys { cardNumber.normalized.startsWith(it) }
        return matches.entries.singleOrNull()?.value.orEmpty()
    }
}

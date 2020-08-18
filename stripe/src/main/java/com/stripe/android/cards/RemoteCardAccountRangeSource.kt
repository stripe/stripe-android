package com.stripe.android.cards

import com.stripe.android.ApiRequest
import com.stripe.android.StripeRepository
import com.stripe.android.model.CardMetadata

internal class RemoteCardAccountRangeSource(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val cardAccountRangeStore: CardAccountRangeStore
) : CardAccountRangeSource {

    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return Bin.create(cardNumber)?.let { bin ->
            val accountRanges =
                stripeRepository.getCardMetadata(bin, requestOptions).accountRanges

            cardAccountRangeStore.save(bin, accountRanges)

            accountRanges
                .firstOrNull { (binRange) ->
                    binRange.matches(cardNumber)
                }
        }
    }
}

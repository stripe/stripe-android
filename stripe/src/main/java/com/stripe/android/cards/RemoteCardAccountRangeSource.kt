package com.stripe.android.cards

import com.stripe.android.ApiRequest
import com.stripe.android.StripeRepository
import com.stripe.android.model.CardMetadata

internal class RemoteCardAccountRangeSource(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options
) : CardAccountRangeSource {

    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return cardNumber
            .take(BIN_LENGTH)
            .takeIf {
                it.length == BIN_LENGTH
            }?.let { bin ->
                stripeRepository.getCardMetadata(bin, requestOptions).accountRanges
                    .firstOrNull { (binRange) ->
                        binRange.matches(cardNumber)
                    }
            }
    }

    private companion object {
        private const val BIN_LENGTH = 6
    }
}

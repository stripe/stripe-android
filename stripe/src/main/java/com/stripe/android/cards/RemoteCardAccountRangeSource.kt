package com.stripe.android.cards

import com.stripe.android.ApiRequest
import com.stripe.android.StripeRepository
import com.stripe.android.model.AccountRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
internal class RemoteCardAccountRangeSource(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val cardAccountRangeStore: CardAccountRangeStore
) : CardAccountRangeSource {

    private val mutableLoading = MutableStateFlow(false)

    override val loading: Flow<Boolean>
        get() = mutableLoading

    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return cardNumber.bin?.let { bin ->
            mutableLoading.value = true

            val accountRanges =
                stripeRepository.getCardMetadata(bin, requestOptions)?.accountRanges.orEmpty()
            cardAccountRangeStore.save(bin, accountRanges)

            mutableLoading.value = false

            accountRanges
                .firstOrNull { (binRange) ->
                    binRange.matches(cardNumber)
                }
        }
    }
}

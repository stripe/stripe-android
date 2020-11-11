package com.stripe.android.cards

import com.stripe.android.AnalyticsEvent
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class RemoteCardAccountRangeSource(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val cardAccountRangeStore: CardAccountRangeStore,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequest.Factory,
    private val analyticsDataFactory: AnalyticsDataFactory
) : CardAccountRangeSource {

    private val mutableLoading = MutableStateFlow(false)

    override val loading: Flow<Boolean>
        get() = mutableLoading

    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return cardNumber.bin?.let { bin ->
            mutableLoading.value = true

            val accountRanges = stripeRepository.getCardMetadata(bin, requestOptions)?.accountRanges.orEmpty()
            cardAccountRangeStore.save(bin, accountRanges)

            mutableLoading.value = false

            when {
                accountRanges.isNotEmpty() -> {
                    val matchedAccountRange = accountRanges
                        .firstOrNull { (binRange) ->
                            binRange.matches(cardNumber)
                        }

                    if (matchedAccountRange == null && cardNumber.isValidLuhn) {
                        onCardMetadataMissingRange()
                    }

                    matchedAccountRange
                }
                else -> null
            }
        }
    }

    private fun onCardMetadataMissingRange() {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createParams(AnalyticsEvent.CardMetadataMissingRange)
            )
        )
    }
}

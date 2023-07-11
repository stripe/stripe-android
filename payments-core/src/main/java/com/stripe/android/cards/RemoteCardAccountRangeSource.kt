package com.stripe.android.cards

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class RemoteCardAccountRangeSource(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val cardAccountRangeStore: CardAccountRangeStore,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
) : CardAccountRangeSource {

    private val _loading = MutableStateFlow(false)

    override val loading: Flow<Boolean>
        get() = _loading

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? {
        return cardNumber.bin?.let { bin ->
            _loading.value = true

            val accountRanges = stripeRepository.getCardMetadata(
                bin = bin,
                options = requestOptions,
            )?.accountRanges.orEmpty()

            if (accountRanges.isNotEmpty()) {
                cardAccountRangeStore.save(bin, accountRanges)
            }

            _loading.value = false

            if (accountRanges.isNotEmpty()) {
                val hasMatch = accountRanges.any { it.binRange.matches(cardNumber) }
                if (!hasMatch && cardNumber.isValidLuhn) {
                    onCardMetadataMissingRange()
                }
            }

            accountRanges.takeIf { it.isNotEmpty() }
        }
    }

    private fun onCardMetadataMissingRange() {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.CardMetadataMissingRange)
        )
    }
}

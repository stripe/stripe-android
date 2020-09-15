package com.stripe.android.cards

import android.content.Context
import com.stripe.android.AnalyticsDataFactory
import com.stripe.android.AnalyticsRequest
import com.stripe.android.AnalyticsRequestExecutor
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository

/**
 * A [CardAccountRangeRepository.Factory] that returns a [DefaultCardAccountRangeRepositoryFactory].
 *
 * Will throw an exception if [PaymentConfiguration] has not been instantiated.
 */
internal class DefaultCardAccountRangeRepositoryFactory(
    context: Context
) : CardAccountRangeRepository.Factory {
    private val appContext = context.applicationContext

    @Throws(IllegalStateException::class)
    override fun create(): CardAccountRangeRepository {
        val paymentConfiguration = PaymentConfiguration.getInstance(
            appContext
        )
        val publishableKey = paymentConfiguration.publishableKey
        val store = DefaultCardAccountRangeStore(appContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = RemoteCardAccountRangeSource(
                StripeApiRepository(
                    appContext,
                    publishableKey
                ),
                ApiRequest.Options(
                    publishableKey
                ),
                DefaultCardAccountRangeStore(appContext),
                AnalyticsRequestExecutor.Default(),
                AnalyticsRequest.Factory(),
                AnalyticsDataFactory(appContext, publishableKey),
                publishableKey
            ),
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }
}

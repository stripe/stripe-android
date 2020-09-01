package com.stripe.android.cards

import android.content.Context
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
        val store = DefaultCardAccountRangeStore(appContext)
        return DefaultCardAccountRangeRepository(
            inMemoryCardAccountRangeSource = InMemoryCardAccountRangeSource(store),
            remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                StripeApiRepository(
                    appContext,
                    paymentConfiguration.publishableKey
                ),
                ApiRequest.Options(
                    paymentConfiguration.publishableKey
                ),
                DefaultCardAccountRangeStore(appContext)
            ),
            staticCardAccountRangeSource = StaticCardAccountRangeSource()
        )
    }
}

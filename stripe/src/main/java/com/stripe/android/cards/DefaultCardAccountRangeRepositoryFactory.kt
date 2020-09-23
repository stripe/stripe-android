package com.stripe.android.cards

import android.content.Context
import com.stripe.android.AnalyticsDataFactory
import com.stripe.android.AnalyticsEvent
import com.stripe.android.AnalyticsRequest
import com.stripe.android.AnalyticsRequestExecutor
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [CardAccountRangeRepository.Factory] that returns a [DefaultCardAccountRangeRepositoryFactory].
 *
 * Will throw an exception if [PaymentConfiguration] has not been instantiated.
 */
internal class DefaultCardAccountRangeRepositoryFactory(
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequest.Factory
) : CardAccountRangeRepository.Factory {
    private val appContext = context.applicationContext

    constructor(context: Context) : this(
        context,
        AnalyticsRequestExecutor.Default(),
        AnalyticsRequest.Factory()
    )

    @Throws(IllegalStateException::class)
    override fun create(): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(appContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = createRemoteCardAccountRangeSource(),
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }

    private fun createRemoteCardAccountRangeSource(): CardAccountRangeSource {
        return runCatching {
            PaymentConfiguration.getInstance(
                appContext
            ).publishableKey
        }.onSuccess { publishableKey ->
            fireAnalyticsEvent(
                publishableKey,
                AnalyticsEvent.CardMetadataPublishableKeyAvailable
            )
        }.onFailure {
            fireAnalyticsEvent(
                ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY,
                AnalyticsEvent.CardMetadataPublishableKeyUnavailable
            )
        }.fold(
            onSuccess = { publishableKey ->
                RemoteCardAccountRangeSource(
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
                )
            },
            onFailure = {
                NullCardAccountRangeSource()
            }
        )
    }

    private fun fireAnalyticsEvent(
        publishableKey: String,
        event: AnalyticsEvent
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                AnalyticsDataFactory(
                    appContext,
                    publishableKey
                ).createParams(event),
                ApiRequest.Options(publishableKey)
            )
        )
    }

    private class NullCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? = null

        override val loading: Flow<Boolean> = flowOf(false)
    }
}

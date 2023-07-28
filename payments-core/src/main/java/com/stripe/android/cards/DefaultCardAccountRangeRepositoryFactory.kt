package com.stripe.android.cards

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [CardAccountRangeRepository.Factory] that returns a [DefaultCardAccountRangeRepositoryFactory].
 *
 * Will throw an exception if [PaymentConfiguration] has not been instantiated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeRepositoryFactory(
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor
) : CardAccountRangeRepository.Factory {
    private val appContext = context.applicationContext

    constructor(context: Context) : this(
        context,
        DefaultAnalyticsRequestExecutor(context)
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

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        val store = DefaultCardAccountRangeStore(appContext)
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = RemoteCardAccountRangeSource(
                stripeRepository,
                ApiRequest.Options(
                    publishableKey
                ),
                DefaultCardAccountRangeStore(appContext),
                DefaultAnalyticsRequestExecutor(appContext),
                PaymentAnalyticsRequestFactory(appContext, publishableKey)
            ),
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
                PaymentAnalyticsEvent.CardMetadataPublishableKeyAvailable
            )
        }.onFailure {
            fireAnalyticsEvent(
                ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY,
                PaymentAnalyticsEvent.CardMetadataPublishableKeyUnavailable
            )
        }.fold(
            onSuccess = { publishableKey ->
                RemoteCardAccountRangeSource(
                    StripeApiRepository(
                        appContext,
                        { publishableKey }
                    ),
                    ApiRequest.Options(
                        publishableKey
                    ),
                    DefaultCardAccountRangeStore(appContext),
                    DefaultAnalyticsRequestExecutor(appContext),
                    PaymentAnalyticsRequestFactory(appContext, publishableKey)
                )
            },
            onFailure = {
                NullCardAccountRangeSource()
            }
        )
    }

    private fun fireAnalyticsEvent(
        publishableKey: String,
        event: PaymentAnalyticsEvent
    ) {
        analyticsRequestExecutor.executeAsync(
            PaymentAnalyticsRequestFactory(
                appContext,
                publishableKey
            ).createRequest(event)
        )
    }

    private class NullCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): List<AccountRange>? = null

        override val loading: Flow<Boolean> = flowOf(false)
    }
}

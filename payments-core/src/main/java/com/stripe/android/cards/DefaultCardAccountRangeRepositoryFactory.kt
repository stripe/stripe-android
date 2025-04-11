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
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Named

/**
 * A [CardAccountRangeRepository.Factory] that returns a [DefaultCardAccountRangeRepositoryFactory].
 *
 * Will throw an exception if [PaymentConfiguration] has not been instantiated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeRepositoryFactory @Inject constructor(
    context: Context,
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String>,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
) : CardAccountRangeRepository.Factory {
    private val appContext = context.applicationContext
    private val cardAccountRangeRepository = lazy {
        val store = InMemoryCardAccountRangeStore()
        DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = createRemoteCardAccountRangeSource(store),
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }

    constructor(context: Context) : this(
        context,
        emptySet(),
        DefaultAnalyticsRequestExecutor(),
    )

    @Throws(IllegalStateException::class)
    override fun create(): CardAccountRangeRepository {
        return cardAccountRangeRepository.value
    }

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        val store = InMemoryCardAccountRangeStore()
        return DefaultCardAccountRangeRepository(
            inMemorySource = InMemoryCardAccountRangeSource(store),
            remoteSource = RemoteCardAccountRangeSource(
                stripeRepository,
                ApiRequest.Options(
                    publishableKey
                ),
                store,
                DefaultAnalyticsRequestExecutor(),
                PaymentAnalyticsRequestFactory(appContext, publishableKey, productUsageTokens)
            ),
            staticSource = StaticCardAccountRangeSource(),
            store = store
        )
    }

    private fun createRemoteCardAccountRangeSource(
        store: CardAccountRangeStore
    ): CardAccountRangeSource {
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
                    store,
                    DefaultAnalyticsRequestExecutor(),
                    PaymentAnalyticsRequestFactory(appContext, publishableKey, productUsageTokens)
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
                publishableKey,
                productUsageTokens,
            ).createRequest(event)
        )
    }

    private class NullCardAccountRangeSource : CardAccountRangeSource {
        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): List<AccountRange>? = null

        override val loading: StateFlow<Boolean> = stateFlowOf(false)
    }
}

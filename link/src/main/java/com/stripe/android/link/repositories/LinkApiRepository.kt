package com.stripe.android.link.repositories

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Repository that uses [StripeRepository] for Link services.
 */
@Singleton
internal class LinkApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val stripeRepository: StripeRepository,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext
) : LinkRepository {

    override suspend fun lookupConsumer(email: String): ConsumerSessionLookup? =
        withContext(workContext) {
            kotlin.runCatching {
                stripeRepository.lookupConsumerSession(
                    email,
                    ApiRequest.Options(
                        publishableKeyProvider(),
                        stripeAccountIdProvider()
                    )
                )
            }.onFailure {
                logger.error("Error looking up consumer", it)
            }.getOrNull()
        }
}

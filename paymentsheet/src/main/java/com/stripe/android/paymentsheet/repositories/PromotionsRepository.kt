package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Repository for fetching and caching payment method promotions.
 * Allows async fetching during PaymentElement load without blocking,
 * with results accessible later by other components.
 */
@Singleton
internal class PromotionsRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext,
) {
    private var promotionsDeferred: Deferred<Result<PaymentMethodMessagePromotionList>>? = null

    /**
     * Starts fetching promotions asynchronously without blocking.
     * This should be called early (e.g., during PaymentElement load) to prefetch data.
     *
     * @param amount The payment amount in the smallest currency unit
     * @param currency The ISO currency code (e.g., "usd")
     * @param country The country code (e.g., "US")
     * @param locale The locale to use (defaults to device locale)
     */
    fun prefetchPromotions(
        amount: Int,
        currency: String,
        country: String?,
        locale: String = Locale.getDefault().language,
    ) {
        // Only fetch once per session - don't restart if already in progress
        if (promotionsDeferred != null) {
            return
        }

        promotionsDeferred = CoroutineScope(workContext).async {
            stripeRepository.retrievePaymentMethodMessageForPaymentSheet(
                amount = amount,
                currency = currency,
                country = country,
                locale = locale,
                requestOptions = ApiRequest.Options(
                    apiKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccount = lazyPaymentConfig.get().stripeAccountId
                )
            ).onFailure { error ->
                logger.error("Failed to fetch promotions", error)
            }
        }
    }

    /**
     * Retrieves the cached promotions result if available.
     * If promotions are still loading, this will wait for them to complete.
     *
     * @return The promotions list if successful, null if failed or not yet fetched
     */
    suspend fun getPromotions(): PaymentMethodMessagePromotionList? {
        return promotionsDeferred?.await()?.getOrNull()
    }

    /**
     * Retrieves the cached promotions result only if already completed.
     * Does not wait if still loading.
     *
     * @return The promotions list if successful and completed, null otherwise
     */
    fun getPromotionsIfAvailable(): PaymentMethodMessagePromotionList? {
        return promotionsDeferred?.takeIf { it.isCompleted }?.getCompleted()?.getOrNull()
    }

    /**
     * Clears the cached promotions data.
     * Call this when starting a new session or when payment parameters change.
     */
    fun clear() {
        promotionsDeferred = null
    }
}

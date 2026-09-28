package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

/**
 * Resolves the adaptive-pricing flag images for a [CheckoutSessionResponse].
 *
 * Flags are keyed by uppercase currency code, so when the response's integration and local
 * currencies are both already present in the [cached] map (i.e. the currencies haven't changed
 * across a mutation such as applying a promotion code), the cached images are reused instead of
 * being re-downloaded. A failed fetch reports [PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed]
 * for each missing flag and leaves the images null so a later mutation can retry.
 */
internal class FlagImageResolver @Inject constructor(
    private val flagImageRepository: FlagImageRepository,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
) {
    suspend fun resolve(
        response: CheckoutSessionResponse,
        cached: Map<String, Bitmap>?,
    ): Map<String, Bitmap>? {
        val adaptivePricingInfo = response.adaptivePricingInfo ?: return null
        val localOption = adaptivePricingInfo.localCurrencyOptions.firstOrNull() ?: return null
        val integrationCurrency = adaptivePricingInfo.integrationCurrency
        val localCurrency = localOption.currency

        if (cached != null &&
            cached.containsKey(integrationCurrency.uppercase()) &&
            cached.containsKey(localCurrency.uppercase())
        ) {
            return cached
        }

        val result = flagImageRepository.fetch(
            integrationCurrencyCode = integrationCurrency,
            localCurrencyCode = localCurrency,
        )
        for (failure in result.failures) {
            val event = PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed(
                countryCode = failure.countryCode,
                url = failure.url,
            )
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                )
            )
        }
        return result.images
    }
}

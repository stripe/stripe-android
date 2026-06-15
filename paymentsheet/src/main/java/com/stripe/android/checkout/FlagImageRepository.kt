package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

internal class FlagImageRepository(
    private val imageLoader: StripeImageLoader,
    private val displayDensity: Float,
) {
    suspend fun fetch(
        integrationCurrencyCode: String,
        localCurrencyCode: String,
    ): PrefetchResult {
        val integrationCountry = currencyCodeToCountryCode(integrationCurrencyCode)
        val localCountry = currencyCodeToCountryCode(localCurrencyCode)

        if (integrationCountry == null || localCountry == null) {
            return PrefetchResult(images = null, failures = emptyList())
        }

        val images = mutableMapOf<String, Bitmap>()
        val failures = mutableListOf<PrefetchFailure>()

        coroutineScope {
            val dpr = displayDensity.roundToInt().coerceIn(1, 4)

            val integrationUrl = buildFlagUrl(integrationCountry, dpr)
            val localUrl = buildFlagUrl(localCountry, dpr)

            val integrationDeferred = async { imageLoader.load(integrationUrl) }
            val localDeferred = async { imageLoader.load(localUrl) }

            val integrationResult = integrationDeferred.await()
            val localResult = localDeferred.await()

            val integrationBitmap = integrationResult.getOrNull()
            val localBitmap = localResult.getOrNull()

            if (integrationBitmap == null) {
                failures.add(PrefetchFailure(countryCode = integrationCountry, url = integrationUrl))
            }
            if (localBitmap == null) {
                failures.add(PrefetchFailure(countryCode = localCountry, url = localUrl))
            }

            if (integrationBitmap != null && localBitmap != null) {
                images[integrationCurrencyCode.uppercase()] = integrationBitmap
                images[localCurrencyCode.uppercase()] = localBitmap
            }
        }

        return PrefetchResult(
            images = if (failures.isEmpty()) images else null,
            failures = failures,
        )
    }

    internal data class PrefetchResult(
        val images: Map<String, Bitmap>?,
        val failures: List<PrefetchFailure>,
    )

    internal data class PrefetchFailure(
        val countryCode: String,
        val url: String,
    )

    companion object {
        private const val FLAGS_BASE = "https://b.stripecdn.com/ocs-mobile/assets/flags/"

        internal fun currencyCodeToCountryCode(currencyCode: String): String? {
            val code = currencyCode.uppercase()
            return when {
                code == "EUR" -> "EU"
                code.length >= 2 && !code.startsWith("X") -> code.substring(0, 2)
                else -> null
            }
        }

        internal fun buildFlagUrl(countryCode: String, dpr: Int): String {
            return "https://img.stripecdn.com/cdn-cgi/image/format=auto,height=16,dpr=$dpr/$FLAGS_BASE$countryCode.png"
        }
    }
}

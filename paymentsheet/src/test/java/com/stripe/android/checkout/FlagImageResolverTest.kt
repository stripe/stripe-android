package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class FlagImageResolverTest {

    @Test
    fun `resolve returns null and skips fetch when there is no adaptive pricing info`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(adaptivePricingInfo = null)

        val result = resolver.resolve(response, cached = null)

        assertThat(result).isNull()
        // No load calls are recorded; the scenario's ensureAllEventsConsumed verifies the skip.
    }

    @Test
    fun `resolve returns null and skips fetch when there are no local currency options`() = runScenario {
        val response = responseWith(integrationCurrency = "usd", localCurrencies = emptyList())

        val result = resolver.resolve(response, cached = null)

        assertThat(result).isNull()
    }

    @Test
    fun `resolve fetches flag images for both currencies on the first resolve`() = runScenario {
        val response = responseWith(integrationCurrency = "usd", localCurrencies = listOf("eur"))

        val result = resolver.resolve(response, cached = null)

        assertThat(result?.keys).containsExactly("USD", "EUR")
        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 3),
            FlagImageRepository.buildFlagUrl("EU", dpr = 3),
        )
    }

    @Test
    fun `resolve reuses cached images when both currencies are unchanged`() = runScenario {
        val response = responseWith(integrationCurrency = "usd", localCurrencies = listOf("eur"))
        val cached = resolver.resolve(response, cached = null)
        loader.awaitLoadedUrls(2)

        val result = resolver.resolve(response, cached = cached)

        assertThat(result).isSameInstanceAs(cached)
        // No further load calls; the scenario's ensureAllEventsConsumed verifies the cache hit.
    }

    @Test
    fun `resolve refetches when the integration currency changes`() = runScenario {
        val cached = mapOf("USD" to bitmap(), "EUR" to bitmap())
        val response = responseWith(integrationCurrency = "gbp", localCurrencies = listOf("eur"))

        val result = resolver.resolve(response, cached = cached)

        assertThat(result?.keys).containsExactly("GBP", "EUR")
        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("GB", dpr = 3),
            FlagImageRepository.buildFlagUrl("EU", dpr = 3),
        )
    }

    @Test
    fun `resolve refetches when the local currency changes`() = runScenario {
        val cached = mapOf("USD" to bitmap(), "EUR" to bitmap())
        val response = responseWith(integrationCurrency = "usd", localCurrencies = listOf("gbp"))

        val result = resolver.resolve(response, cached = cached)

        assertThat(result?.keys).containsExactly("USD", "GBP")
        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 3),
            FlagImageRepository.buildFlagUrl("GB", dpr = 3),
        )
    }

    @Test
    fun `resolve refetches when the cache only covers one of the currencies`() = runScenario {
        val cached = mapOf("USD" to bitmap())
        val response = responseWith(integrationCurrency = "usd", localCurrencies = listOf("eur"))

        val result = resolver.resolve(response, cached = cached)

        assertThat(result?.keys).containsExactly("USD", "EUR")
        assertThat(loader.awaitLoadedUrls(2)).hasSize(2)
    }

    @Test
    fun `resolve returns null without reporting analytics when a currency has no country code`() = runScenario {
        // xaf maps to no country, so the repository skips the download and reports no failures.
        // Unlike a download failure, this path must not emit analytics.
        val response = responseWith(integrationCurrency = "xaf", localCurrencies = listOf("eur"))

        val result = resolver.resolve(response, cached = null)

        assertThat(result).isNull()
        assertThat(analyticsRequestExecutor.getExecutedRequests()).isEmpty()
        // No load calls; the scenario's ensureAllEventsConsumed verifies the skip.
    }

    @Test
    fun `resolve reports analytics and returns null when a flag download fails`() = runScenario(
        failingUrls = setOf(FlagImageRepository.buildFlagUrl("US", dpr = 3)),
    ) {
        val response = responseWith(integrationCurrency = "usd", localCurrencies = listOf("eur"))

        val result = resolver.resolve(response, cached = null)

        assertThat(result).isNull()
        val requests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(requests).hasSize(1)
        assertThat(requests.first().params["event"])
            .isEqualTo("elements.adaptive_pricing.flag_image_load.failed")
        // Both currencies are fetched (US fails, EU succeeds); consume them for the scenario's check.
        loader.awaitLoadedUrls(2)
    }

    private fun responseWith(
        integrationCurrency: String,
        localCurrencies: List<String>,
    ): CheckoutSessionResponse {
        return CheckoutSessionResponseFactory.create(
            adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
                activePresentmentCurrency = localCurrencies.firstOrNull() ?: integrationCurrency,
                integrationAmount = 5099,
                integrationCurrency = integrationCurrency,
                localCurrencyOptions = localCurrencies.map { currency ->
                    CheckoutSessionResponse.LocalCurrencyOption(
                        amount = 4594,
                        conversionMarkupBps = 400,
                        currency = currency,
                        presentmentExchangeRate = "0.900961",
                    )
                },
            ),
        )
    }

    private fun bitmap(): Bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)

    // Awaits [count] load calls and returns the requested URLs. The repository fetches the two
    // flags concurrently, so callers compare against a set rather than an ordered list.
    private suspend fun FakeStripeImageLoader.awaitLoadedUrls(count: Int): Set<String> =
        buildSet { repeat(count) { add(awaitLoadCall().url) } }

    private fun runScenario(
        failingUrls: Set<String> = emptySet(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val loader = FakeStripeImageLoader(
            loadResult = Result.success(bitmap()),
            loadResultByUrl = failingUrls.associateWith {
                Result.failure<Bitmap?>(RuntimeException("Download failed"))
            },
        )
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val resolver = FlagImageResolver(
            flagImageRepository = FlagImageRepository(imageLoader = loader, displayDensity = 3f),
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = application,
                publishableKey = "pk_test_123",
            ),
        )

        Scenario(
            resolver = resolver,
            loader = loader,
            analyticsRequestExecutor = analyticsRequestExecutor,
        ).block()

        loader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val resolver: FlagImageResolver,
        val loader: FakeStripeImageLoader,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
    )
}

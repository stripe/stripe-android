package com.stripe.android.checkout

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class FlagImageRepositoryTest {

    @Test
    fun `fetch returns images for both currencies when both downloads succeed`() = runScenario {
        val result = repository.fetch("usd", "eur")

        assertThat(result.images).isNotNull()
        assertThat(result.images!!.keys).containsExactly("USD", "EUR")
        assertThat(result.failures).isEmpty()
        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 3),
            FlagImageRepository.buildFlagUrl("EU", dpr = 3),
        )
    }

    @Test
    fun `fetch returns null images and a failure when the integration download fails`() = runScenario(
        failingUrls = setOf(FlagImageRepository.buildFlagUrl("US", dpr = 3)),
    ) {
        val result = repository.fetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(1)
        assertThat(result.failures[0].countryCode).isEqualTo("US")
        assertThat(result.failures[0].url).isEqualTo(FlagImageRepository.buildFlagUrl("US", dpr = 3))
        loader.awaitLoadedUrls(2)
    }

    @Test
    fun `fetch returns null images and a failure when the local download fails`() = runScenario(
        failingUrls = setOf(FlagImageRepository.buildFlagUrl("EU", dpr = 3)),
    ) {
        val result = repository.fetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(1)
        assertThat(result.failures[0].countryCode).isEqualTo("EU")
        loader.awaitLoadedUrls(2)
    }

    @Test
    fun `fetch returns null images and two failures when both downloads fail`() = runScenario(
        failingUrls = setOf(
            FlagImageRepository.buildFlagUrl("US", dpr = 3),
            FlagImageRepository.buildFlagUrl("EU", dpr = 3),
        ),
    ) {
        val result = repository.fetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(2)
        loader.awaitLoadedUrls(2)
    }

    @Test
    fun `fetch skips download and returns empty failures when the integration currency has no country`() =
        runScenario {
            val result = repository.fetch("xaf", "usd")

            assertThat(result.images).isNull()
            assertThat(result.failures).isEmpty()
            // No load calls; the scenario's ensureAllEventsConsumed verifies the skip.
        }

    @Test
    fun `fetch skips download and returns empty failures when the local currency has no country`() =
        runScenario {
            val result = repository.fetch("usd", "xpf")

            assertThat(result.images).isNull()
            assertThat(result.failures).isEmpty()
        }

    @Test
    fun `EUR maps to EU country code`() {
        assertThat(FlagImageRepository.currencyCodeToCountryCode("EUR")).isEqualTo("EU")
    }

    @Test
    fun `USD maps to US country code`() {
        assertThat(FlagImageRepository.currencyCodeToCountryCode("USD")).isEqualTo("US")
    }

    @Test
    fun `XAF returns null country code`() {
        assertThat(FlagImageRepository.currencyCodeToCountryCode("XAF")).isNull()
    }

    @Test
    fun `buildFlagUrl produces correct CDN proxy URL`() {
        val url = FlagImageRepository.buildFlagUrl("US", dpr = 3)
        assertThat(url).isEqualTo(
            "https://img.stripecdn.com/cdn-cgi/image/format=auto,height=16,dpr=3/" +
                "https://b.stripecdn.com/ocs-mobile/assets/flags/US.png"
        )
    }

    @Test
    fun `fetch rounds the display density to the nearest dpr`() = runScenario(displayDensity = 2.6f) {
        repository.fetch("usd", "eur")

        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 3),
            FlagImageRepository.buildFlagUrl("EU", dpr = 3),
        )
    }

    @Test
    fun `fetch clamps a sub-1 display density up to a dpr of 1`() = runScenario(displayDensity = 0.4f) {
        repository.fetch("usd", "eur")

        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 1),
            FlagImageRepository.buildFlagUrl("EU", dpr = 1),
        )
    }

    @Test
    fun `fetch clamps a large display density down to a dpr of 4`() = runScenario(displayDensity = 10f) {
        repository.fetch("usd", "eur")

        assertThat(loader.awaitLoadedUrls(2)).containsExactly(
            FlagImageRepository.buildFlagUrl("US", dpr = 4),
            FlagImageRepository.buildFlagUrl("EU", dpr = 4),
        )
    }

    // Awaits [count] load calls and returns the requested URLs. The repository fetches the two
    // flags concurrently, so callers compare against a set rather than an ordered list.
    private suspend fun FakeStripeImageLoader.awaitLoadedUrls(count: Int): Set<String> =
        buildSet { repeat(count) { add(awaitLoadCall().url) } }

    private fun runScenario(
        failingUrls: Set<String> = emptySet(),
        displayDensity: Float = 3f,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val loader = FakeStripeImageLoader(
            loadResult = Result.success(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)),
            loadResultByUrl = failingUrls.associateWith {
                Result.failure<Bitmap?>(RuntimeException("Download failed"))
            },
        )
        val repository = FlagImageRepository(imageLoader = loader, displayDensity = displayDensity)

        Scenario(repository = repository, loader = loader).block()

        loader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val repository: FlagImageRepository,
        val loader: FakeStripeImageLoader,
    )
}

package com.stripe.android.checkout

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FlagImageRepositoryTest {

    @Test
    fun `both downloads succeed - returns images for both currencies`() = runTest {
        val fakeLoader = FakeStripeImageLoader()
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("usd", "eur")

        assertThat(result.images).isNotNull()
        assertThat(result.images!!.keys).containsExactly("USD", "EUR")
        assertThat(result.failures).isEmpty()
    }

    @Test
    fun `integration download fails - returns null images and failure`() = runTest {
        val fakeLoader = FakeStripeImageLoader(
            failingUrls = setOf(FlagImageRepository.buildFlagUrl("US", dpr = 3)),
        )
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(1)
        assertThat(result.failures[0].countryCode).isEqualTo("US")
        assertThat(result.failures[0].url).isEqualTo(FlagImageRepository.buildFlagUrl("US", dpr = 3))
    }

    @Test
    fun `local download fails - returns null images and failure`() = runTest {
        val fakeLoader = FakeStripeImageLoader(
            failingUrls = setOf(FlagImageRepository.buildFlagUrl("EU", dpr = 3)),
        )
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(1)
        assertThat(result.failures[0].countryCode).isEqualTo("EU")
    }

    @Test
    fun `both downloads fail - returns null images and two failures`() = runTest {
        val fakeLoader = FakeStripeImageLoader(
            failingUrls = setOf(
                FlagImageRepository.buildFlagUrl("US", dpr = 3),
                FlagImageRepository.buildFlagUrl("EU", dpr = 3),
            ),
        )
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("usd", "eur")

        assertThat(result.images).isNull()
        assertThat(result.failures).hasSize(2)
    }

    @Test
    fun `X-currency integration code - skips prefetch and returns empty failures`() = runTest {
        val fakeLoader = FakeStripeImageLoader()
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("xaf", "usd")

        assertThat(result.images).isNull()
        assertThat(result.failures).isEmpty()
    }

    @Test
    fun `X-currency local code - skips prefetch and returns empty failures`() = runTest {
        val fakeLoader = FakeStripeImageLoader()
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        val result = repository.prefetch("usd", "xpf")

        assertThat(result.images).isNull()
        assertThat(result.failures).isEmpty()
    }

    @Test
    fun `get returns cached bitmap after successful prefetch`() = runTest {
        val fakeLoader = FakeStripeImageLoader()
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        repository.prefetch("usd", "eur")

        assertThat(repository.get("USD")).isNotNull()
        assertThat(repository.get("EUR")).isNotNull()
    }

    @Test
    fun `get returns null when prefetch failed`() = runTest {
        val fakeLoader = FakeStripeImageLoader(
            failingUrls = setOf(FlagImageRepository.buildFlagUrl("US", dpr = 3)),
        )
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        repository.prefetch("usd", "eur")

        assertThat(repository.get("USD")).isNull()
        assertThat(repository.get("EUR")).isNull()
    }

    @Test
    fun `get returns null for currency not prefetched`() = runTest {
        val fakeLoader = FakeStripeImageLoader()
        val repository = FlagImageRepository(fakeLoader, displayDensity = 3f)

        repository.prefetch("usd", "eur")

        assertThat(repository.get("GBP")).isNull()
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
}

private class FakeStripeImageLoader(
    private val failingUrls: Set<String> = emptySet(),
) : StripeImageLoader {
    override suspend fun load(url: String, width: Int, height: Int): Result<Bitmap?> {
        return load(url)
    }

    override suspend fun load(url: String): Result<Bitmap?> {
        return if (url in failingUrls) {
            Result.failure(RuntimeException("Download failed"))
        } else {
            Result.success(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))
        }
    }

    override suspend fun get(url: String): Result<Bitmap?> {
        return Result.success(null)
    }
}

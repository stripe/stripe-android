package com.stripe.android.common.taptoadd

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultTapToAddImageRepositoryTest {

    private val testBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `get returns null for unknown card brand`() = runScenario {
        assertThat(repository.get(CardBrand.Unknown)).isNull()
    }

    @Test
    fun `load for unknown card brand returns null`() = runScenario {
        val result = repository.load(CardBrand.Unknown).await()
        assertThat(result).isNull()
    }

    @Test
    fun `load for unknown card brand does not call imageLoader`() = runScenario {
        repository.load(CardBrand.Unknown).await()
        verify(imageLoader, times(5)).load(any())
    }

    @Test
    fun `load for known brand returns CardArt when image loader succeeds`() = runScenario(
        imageResult = Result.success(testBitmap),
    ) {
        val cardArt = repository.load(CardBrand.Visa).await()
        assertThat(cardArt).isNotNull()
        assertThat(cardArt!!.bitmap).isEqualTo(testBitmap)
        assertThat(cardArt.textColor).isEqualTo(Color.White)
    }

    @Test
    fun `load for known brand returns null when image loader fails`() = runScenario(
        imageResult = Result.failure(RuntimeException("load failed")),
    ) {
        val cardArt = repository.load(CardBrand.Visa).await()
        assertThat(cardArt).isNull()
    }

    @Test
    fun `get returns cached CardArt after load completes`() = runScenario(
        imageResult = Result.success(testBitmap),
    ) {
        repository.load(CardBrand.MasterCard).await()
        val cached = repository.get(CardBrand.MasterCard)

        assertThat(cached).isNotNull()
        assertThat(cached!!.bitmap).isEqualTo(testBitmap)
        assertThat(cached.textColor).isEqualTo(Color.Black)
    }

    @Test
    fun `preload populates cache so get returns CardArt for supported brands`() = runScenario(
        imageResult = Result.success(testBitmap),
    ) {
        assertThat(repository.get(CardBrand.Visa)).isNotNull()
        assertThat(repository.get(CardBrand.MasterCard)).isNotNull()
        assertThat(repository.get(CardBrand.Discover)).isNotNull()
        assertThat(repository.get(CardBrand.AmericanExpress)).isNotNull()
        assertThat(repository.get(CardBrand.JCB)).isNotNull()
    }

    @Test
    fun `preloaded card art has correct text colors per brand`() = runScenario(
        imageResult = Result.success(testBitmap),
    ) {
        assertThat(repository.get(CardBrand.Visa)!!.textColor).isEqualTo(Color.White)
        assertThat(repository.get(CardBrand.MasterCard)!!.textColor).isEqualTo(Color.Black)
        assertThat(repository.get(CardBrand.Discover)!!.textColor).isEqualTo(Color.White)
        assertThat(repository.get(CardBrand.AmericanExpress)!!.textColor).isEqualTo(Color.White)
        assertThat(repository.get(CardBrand.JCB)!!.textColor).isEqualTo(Color.White)
    }

    @Test
    fun `load when already cached returns same art without calling imageLoader again`() = runScenario(
        imageResult = Result.success(testBitmap),
      ) {
        val first = repository.load(CardBrand.Visa).await()
        val second = repository.load(CardBrand.Visa).await()
        assertThat(first).isNotNull()
        assertThat(second).isEqualTo(first)
        assertThat(repository.get(CardBrand.Visa)).isEqualTo(first)
        verify(imageLoader, times(5)).load(any())
      }

    @Test
    fun `concurrent load for same brand returns same result`() = runScenario(
        imageResult = Result.success(testBitmap),
    ) {
        val (result1, result2) = coroutineScope {
            val d1 = async { repository.load(CardBrand.JCB).await() }
            val d2 = async { repository.load(CardBrand.JCB).await() }
            d1.await() to d2.await()
        }
        assertThat(result1).isNotNull()
        assertThat(result2).isNotNull()
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `get returns null for brand before load is invoked`() = runTest(testDispatcher) {
        val imageLoader = mock<StripeImageLoader>().apply {
            whenever(load(any())).thenReturn(Result.success(testBitmap))
        }
        val repository = DefaultTapToAddImageRepository(
            coroutineContext = testDispatcher,
            viewModelScope = this,
            imageLoader = imageLoader,
        )
        assertThat(repository.get(CardBrand.Visa)).isNull()
        advanceUntilIdle()
        assertThat(repository.get(CardBrand.Visa)).isNotNull()
    }

    private fun runScenario(
        imageResult: Result<Bitmap?> = Result.success(testBitmap),
        block: suspend Scenario.() -> Unit = {},
    ) = runTest(testDispatcher) {
        val imageLoader = mock<StripeImageLoader>().apply {
            whenever(load(any())).thenReturn(imageResult)
        }

        val repository = DefaultTapToAddImageRepository(
            coroutineContext = testDispatcher,
            viewModelScope = this,
            imageLoader = imageLoader,
        )

        advanceUntilIdle()

        block(Scenario(repository = repository, imageLoader = imageLoader))
    }

    private class Scenario(
        val repository: TapToAddImageRepository,
        val imageLoader: StripeImageLoader,
    )
}

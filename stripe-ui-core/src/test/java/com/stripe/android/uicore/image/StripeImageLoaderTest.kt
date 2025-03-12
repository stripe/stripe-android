package com.stripe.android.uicore.image

import android.content.Context
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.URL

class StripeImageLoaderTest {

    private val context = mock<Context>()
    private val logger = mock<Logger>()
    private val memoryCache = mock<ImageLruMemoryCache>()
    private val networkImageDecoder = mock<NetworkImageDecoder>()
    private val diskCache = mock<ImageLruDiskCache>()

    private val image = LoadedImage(
        bitmap = mock<Bitmap>(),
        contentType = LoadedImage.ContentType.Known.Png,
    )
    private val key: String = "https://image-key.com"
    private val keyNoSize: String = "https://image-key-no-size.com"
    private val imageNoSize = LoadedImage(
        bitmap = mock<Bitmap>(),
        contentType = LoadedImage.ContentType.Known.Png,
    )

    private val imageLoader = StripeImageLoader(
        context,
        logger,
        memoryCache,
        networkImageDecoder,
        diskCache
    )

    @Test
    fun `load - if image available in memory cache, return it and don't reach other sources`() =
        runTest {
            memoryCacheReturns(key, image)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(image.bitmap)
            verifyNoInteractions(networkImageDecoder)

            memoryCacheReturns(keyNoSize, imageNoSize)

            val bitmapUnknownSize = imageLoader.load(keyNoSize)

            assertThat(bitmapUnknownSize.getOrThrow()).isEqualTo(imageNoSize.bitmap)
            verifyNoInteractions(networkImageDecoder)
        }

    @Test
    fun `load - if image available in disk cache, return it and cache it in memory`() =
        runTest {
            memoryCacheReturns(key, null)
            diskCacheReturns(key, image)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(image.bitmap)
            verify(memoryCache).put(key, image)
            verifyNoInteractions(networkImageDecoder)

            memoryCacheReturns(keyNoSize, null)
            diskCacheReturns(keyNoSize, imageNoSize)

            val bitmapUnknownSize = imageLoader.load(keyNoSize)

            assertThat(bitmapUnknownSize.getOrThrow()).isEqualTo(imageNoSize.bitmap)
            verify(memoryCache).put(keyNoSize, imageNoSize)
            verifyNoInteractions(networkImageDecoder)
        }

    @Test
    fun `load - if image available in network cache, return it and cache it on memory and disk`() =
        runTest {
            memoryCacheReturns(key, null)
            diskCacheReturns(key, null)
            networkCacheReturns(key, 400, 600, image)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(image.bitmap)
            verify(diskCache).put(key, image)
            verify(memoryCache).put(key, image)

            memoryCacheReturns(keyNoSize, null)
            diskCacheReturns(keyNoSize, null)
            networkCacheReturns(keyNoSize, imageNoSize)

            val bitmapNoSize = imageLoader.load(keyNoSize)

            assertThat(bitmapNoSize.getOrThrow()).isEqualTo(imageNoSize.bitmap)
            verify(diskCache).put(keyNoSize, imageNoSize)
            verify(memoryCache).put(keyNoSize, imageNoSize)
        }

    private fun memoryCacheReturns(key: String, image: LoadedImage?) {
        whenever(memoryCache.get(key)).thenReturn(image)
    }

    private fun diskCacheReturns(key: String, image: LoadedImage?) {
        whenever(diskCache.get(key)).thenReturn(image)
    }

    private suspend fun networkCacheReturns(
        url: String,
        width: Int,
        height: Int,
        image: LoadedImage
    ) {
        whenever(networkImageDecoder.decode(URL(url), width, height)).thenReturn(image)
    }

    private suspend fun networkCacheReturns(
        url: String,
        image: LoadedImage
    ) {
        whenever(networkImageDecoder.decode(URL(url))).thenReturn(image)
    }
}

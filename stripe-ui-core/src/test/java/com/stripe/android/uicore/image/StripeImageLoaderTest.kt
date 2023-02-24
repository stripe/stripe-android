package com.stripe.android.uicore.image

import android.content.Context
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StripeImageLoaderTest {

    private val context = mock<Context>()
    private val logger = mock<Logger>()
    private val memoryCache = mock<ImageLruMemoryCache>()
    private val networkImageDecoder = mock<NetworkImageDecoder>()
    private val diskCache = mock<ImageLruDiskCache>()

    private val mockBitmap = mock<Bitmap>()
    private val key: String = "image-key"
    private val keyNoSize: String = "image-key-no-size"
    private val mockBitmapNoSize = mock<Bitmap>()

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
            memoryCacheReturns(key, mockBitmap)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(mockBitmap)
            verifyNoInteractions(networkImageDecoder)

            memoryCacheReturns(keyNoSize, mockBitmapNoSize)

            val bitmapUnknownSize = imageLoader.load(keyNoSize)

            assertThat(bitmapUnknownSize.getOrThrow()).isEqualTo(mockBitmapNoSize)
            verifyNoInteractions(networkImageDecoder)
        }

    @Test
    fun `load - if image available in disk cache, return it and cache it in memory`() =
        runTest {
            memoryCacheReturns(key, null)
            diskCacheReturns(key, mockBitmap)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(mockBitmap)
            verify(memoryCache).put(key, mockBitmap)
            verifyNoInteractions(networkImageDecoder)

            memoryCacheReturns(keyNoSize, null)
            diskCacheReturns(keyNoSize, mockBitmapNoSize)

            val bitmapUnknownSize = imageLoader.load(keyNoSize)

            assertThat(bitmapUnknownSize.getOrThrow()).isEqualTo(mockBitmapNoSize)
            verify(memoryCache).put(keyNoSize, mockBitmapNoSize)
            verifyNoInteractions(networkImageDecoder)
        }

    @Test
    fun `load - if image available in network cache, return it and cache it on memory and disk`() =
        runTest {
            memoryCacheReturns(key, null)
            diskCacheReturns(key, null)
            networkCacheReturns(key, 400, 600, mockBitmap)

            val bitmap = imageLoader.load(key, 400, 600)

            assertThat(bitmap.getOrThrow()).isEqualTo(mockBitmap)
            verify(diskCache).put(key, mockBitmap)
            verify(memoryCache).put(key, mockBitmap)

            memoryCacheReturns(keyNoSize, null)
            diskCacheReturns(keyNoSize, null)
            networkCacheReturns(keyNoSize, mockBitmapNoSize)

            val bitmapNoSize = imageLoader.load(keyNoSize)

            assertThat(bitmapNoSize.getOrThrow()).isEqualTo(mockBitmapNoSize)
            verify(diskCache).put(keyNoSize, mockBitmapNoSize)
            verify(memoryCache).put(keyNoSize, mockBitmapNoSize)
        }

    private fun memoryCacheReturns(key: String, bitmap: Bitmap?) {
        whenever(memoryCache.getBitmap(key)).thenReturn(bitmap)
    }

    private fun diskCacheReturns(key: String, bitmap: Bitmap?) {
        whenever(diskCache.getBitmap(key)).thenReturn(bitmap)
    }

    private suspend fun networkCacheReturns(
        key: String,
        width: Int,
        height: Int,
        bitmap: Bitmap
    ) {
        whenever(networkImageDecoder.decode(key, width, height)).thenReturn(bitmap)
    }

    private suspend fun networkCacheReturns(
        key: String,
        bitmap: Bitmap
    ) {
        whenever(networkImageDecoder.decode(key)).thenReturn(bitmap)
    }
}

package com.stripe.android.stripe3ds2.utils

import android.graphics.Bitmap
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ImageCacheTest {
    private val cache = ImageCache.Default

    @BeforeTest
    fun before() {
        cache.clear()
    }

    @Test
    fun setBitmap_bitmapIsSet() {
        assertEquals(0, cache.cache.size())
        cache["KEY"] = BITMAP
        assertTrue(cache.cache.size() > 0)
        assertEquals(BITMAP, cache["KEY"])
    }

    @Test
    fun getBitmap_nonExistent_bitmapIsNull() {
        assertNull(cache["KEY"])
    }

    @Test
    fun clear_cacheIsCleared() {
        assertEquals(0, cache.cache.size())
        cache["KEY"] = BITMAP
        assertTrue(cache.cache.size() > 0)
        cache.clear()
        assertEquals(0, cache.cache.size())
    }

    private companion object {
        private val BITMAP = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
    }
}

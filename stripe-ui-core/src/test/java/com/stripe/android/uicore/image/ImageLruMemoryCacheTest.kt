package com.stripe.android.uicore.image

import android.graphics.BitmapFactory
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ImageLruMemoryCacheTest {
    @Test
    fun `should be able to store and retrieve saved image`() {
        val cache = ImageLruMemoryCache()

        assertThat(cache.get(URL)).isNull()

        cache.put(
            key = URL,
            image = LoadedImage(
                contentType = LoadedImage.ContentType.Png,
                bitmap = BitmapFactory.decodeStream(readImage()),
            ),
        )

        val image = cache.get(URL)

        assertThat(image).isNotNull()

        val nonNullImage = requireNotNull(image)

        assertThat(nonNullImage.contentType).isEqualTo(LoadedImage.ContentType.Png)
    }

    private fun readImage() = Unit::class.java.classLoader!!.getResourceAsStream("example.png")

    private companion object {
        const val URL = "https://image"
    }
}

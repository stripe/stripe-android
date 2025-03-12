package com.stripe.android.uicore.image

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ImageLruDiskCacheTest {
    @Test
    fun `should be able to store and retrieve saved image`() {
        val cache = ImageLruDiskCache(
            context = ApplicationProvider.getApplicationContext(),
            cacheFolder = "stripe_image_test_cache"
        )

        assertThat(cache.containsKey(URL)).isFalse()
        assertThat(cache.get(URL)).isNull()

        cache.put(
            key = URL,
            image = LoadedImage(
                contentType = LoadedImage.ContentType.Png,
                bitmap = BitmapFactory.decodeStream(readImage()),
            ),
        )

        assertThat(cache.containsKey(URL)).isTrue()

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

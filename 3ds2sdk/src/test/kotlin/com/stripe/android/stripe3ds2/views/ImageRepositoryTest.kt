package com.stripe.android.stripe3ds2.views

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.utils.ImageCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class ImageRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val imageCache = ImageCache.Default
    private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val imageSupplier = FakeImageSupplier(bitmap)

    private val imageRepository = ImageRepository(
        testDispatcher,
        imageCache,
        imageSupplier
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun getImage_withNullUrl_shouldFail() = testDispatcher.runBlockingTest {
        assertThat(imageRepository.getImage(imageUrl = null))
            .isNull()
    }

    @Test
    fun getImage_whenImageInCache_shouldSucceed() = testDispatcher.runBlockingTest {
        imageCache[IMAGE_URL] = bitmap
        assertThat(imageRepository.getImage(IMAGE_URL))
            .isEqualTo(bitmap)
    }

    @Test
    fun getImage_withValidRemoteImage_shouldSucceed() = testDispatcher.runBlockingTest {
        imageCache.clear()
        assertThat(imageRepository.getImage(IMAGE_URL))
            .isEqualTo(bitmap)
    }

    @Test
    fun getImage_withInvalidRemoteImage_shouldSucceed() = testDispatcher.runBlockingTest {
        val imageRepository = ImageRepository(
            testDispatcher,
            imageCache,
            FakeImageSupplier(null)
        )
        assertThat(imageRepository.getImage("https://example.invalid"))
            .isNull()
    }

    private class FakeImageSupplier(
        private val bitmap: Bitmap?
    ) : ImageRepository.ImageSupplier {
        override suspend fun getBitmap(url: String): Bitmap? {
            return bitmap
        }
    }

    private companion object {
        private const val IMAGE_URL = "https://3ds.selftestplatform.com/images/BankLogo_medium.png"
    }
}

package com.stripe.android.stripe3ds2.views

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.utils.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ImageRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val imageCache = ImageCache.Default
    private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val imageSupplier = FakeImageSupplier(bitmap)

    private val imageRepository = ImageRepository(
        testDispatcher,
        imageCache,
        imageSupplier
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun getImage_withNullUrl_shouldFail() = runTest {
        assertThat(imageRepository.getImage(imageUrl = null))
            .isNull()
    }

    @Test
    fun getImage_whenImageInCache_shouldSucceed() = runTest {
        imageCache[IMAGE_URL] = bitmap
        assertThat(imageRepository.getImage(IMAGE_URL))
            .isEqualTo(bitmap)
    }

    @Test
    fun getImage_withValidRemoteImage_shouldSucceed() = runTest {
        imageCache.clear()
        assertThat(imageRepository.getImage(IMAGE_URL))
            .isEqualTo(bitmap)
    }

    @Test
    fun getImage_withInvalidRemoteImage_shouldSucceed() = runTest {
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

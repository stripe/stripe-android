package com.stripe.android.identity.states

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
internal class LaplacianBlurDetectorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()

    @Test
    fun `calculateBlurOutput returns default score and reports error for unsupported bitmap config`() {
        val detector = LaplacianBlurDetector(context, mockAnalyticsRequestFactory)

        val score = detector.calculateBlurOutput(
            Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.RGBA_F16)
        )

        assertThat(score).isEqualTo(1.0f)
        verify(mockAnalyticsRequestFactory).genericError(
            any(),
            any(),
            eq("Failed to calculate blur score")
        )
    }

    private companion object {
        const val BITMAP_SIZE = 4
    }
}

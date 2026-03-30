package com.stripe.android.common.taptoadd

import android.graphics.Bitmap
import app.cash.turbine.Turbine
import com.stripe.android.uicore.image.StripeImageLoader
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TapToAddStripeImageLoaderTestRule : TestWatcher() {
    private val imageLoadWithUrls = Turbine<String>()
    private val fakeImage = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    suspend fun awaitImageLoadWithUrl() = imageLoadWithUrls.awaitItem()

    override fun starting(description: Description?) {
        TapToAddStripeImageLoaderFactory.override = mock<StripeImageLoader>().apply {
            whenever {
                load(any())
            } doAnswer { invocation ->
                imageLoadWithUrls.add(invocation.getArgument(0))

                Result.success(fakeImage)
            }
        }

        super.starting(description)
    }

    override fun finished(description: Description?) {
        TapToAddStripeImageLoaderFactory.override = null
        imageLoadWithUrls.ensureAllEventsConsumed()
        super.finished(description)
    }
}

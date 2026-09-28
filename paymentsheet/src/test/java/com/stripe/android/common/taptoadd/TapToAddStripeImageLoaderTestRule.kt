package com.stripe.android.common.taptoadd

import android.graphics.Bitmap
import com.stripe.android.testing.FakeStripeImageLoader
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TapToAddStripeImageLoaderTestRule : TestWatcher() {
    private val fakeImage = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    private lateinit var fakeImageLoader: FakeStripeImageLoader

    suspend fun awaitImageLoadWithUrl(): String = fakeImageLoader.awaitLoadCall().url

    override fun starting(description: Description?) {
        fakeImageLoader = FakeStripeImageLoader(loadResult = Result.success(fakeImage))
        TapToAddStripeImageLoaderFactory.override = fakeImageLoader
        super.starting(description)
    }

    override fun finished(description: Description?) {
        TapToAddStripeImageLoaderFactory.override = null
        fakeImageLoader.ensureAllEventsConsumed()
        super.finished(description)
    }
}

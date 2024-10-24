package com.stripe.android.stripecardscan.payment.ml

import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.camera.framework.image.scale
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.toRect
import com.stripe.android.stripecardscan.framework.ResourceFetcher
import com.stripe.android.stripecardscan.test.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SSDOcrTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    @MediumTest
    fun resourceModelExecution_works() = runTest {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
            .scale(Size(600, 375))
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )
        assertNotNull(prediction)

        assertEquals("4242424242424242", prediction.pan)
    }

    @Test
    @MediumTest
    fun resourceModelExecution_worksWithQR() = runTest {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers_qr, null)
            .toBitmap()
            .scale(Size(600, 375))
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )
        assertNotNull(prediction)

        assertEquals("4242424242424242", prediction.pan)
    }

    @Test
    @MediumTest
    fun resourceModelExecution_worksRepeatedly() = runTest {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
            .scale(Size(600, 375))
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )

        assertEquals("4242424242424242", prediction.pan)
    }
}

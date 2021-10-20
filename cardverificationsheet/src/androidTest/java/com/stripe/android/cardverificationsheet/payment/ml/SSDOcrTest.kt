package com.stripe.android.cardverificationsheet.payment.ml

import androidx.core.graphics.drawable.toBitmap
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.cardverificationsheet.framework.ResourceFetcher
import com.stripe.android.cardverificationsheet.framework.image.size
import com.stripe.android.cardverificationsheet.framework.util.toRect
import com.stripe.android.cardverificationsheet.test.R
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SSDOcrTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    /**
     * TODO: this method should use runBlockingTest instead of runBlocking. However, an issue with
     * runBlockingTest currently fails when functions under test use withContext(Dispatchers.IO) or
     * withContext(Dispatchers.Default).
     *
     * See https://github.com/Kotlin/kotlinx.coroutines/issues/1204 for details.
     */
    @Test
    @MediumTest
    fun resourceModelExecution_works() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false),
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect(),
                null,
                "1104",
            ),
            Unit
        )
        assertNotNull(prediction)

        // card number is "3023334877861104"
        assertEquals(SSDOcr.OcrOutcome.Match, prediction.outcome)
    }

    /**
     * TODO: this method should use runBlockingTest instead of runBlocking. However, an issue with
     * runBlockingTest currently fails when functions under test use withContext(Dispatchers.IO) or
     * withContext(Dispatchers.Default).
     *
     * See https://github.com/Kotlin/kotlinx.coroutines/issues/1204 for details.
     */
    @Test
    @MediumTest
    fun resourceModelExecution_worksWithQR() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers_qr, null)
            .toBitmap()
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertFalse(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false),
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect(),
                null,
                "4242",
            ),
            Unit
        )
        assertNotNull(prediction)

        // card number is "4242424242424242"
        assertEquals(SSDOcr.OcrOutcome.Match, prediction.outcome)
    }

    /**
     * TODO: this method should use runBlockingTest instead of runBlocking. However, an issue with
     * runBlockingTest currently fails when functions under test use withContext(Dispatchers.IO) or
     * withContext(Dispatchers.Default).
     *
     * See https://github.com/Kotlin/kotlinx.coroutines/issues/1204 for details.
     */
    @Test
    @MediumTest
    fun resourceModelExecution_worksRepeatedly() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
        val fetcher = SSDOcrModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertFalse(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = SSDOcr.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false),
        ).newInstance()
        assertNotNull(model)

        val prediction1 = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect(),
                null,
                "1104",
            ),
            Unit
        )
        val prediction2 = model.analyze(
            SSDOcr.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect(),
                null,
                "1234",
            ),
            Unit
        )

        // card number is "3023334877861104"

        assertNotNull(prediction1)
        assertEquals(SSDOcr.OcrOutcome.Match, prediction1.outcome)

        assertNotNull(prediction2)
        assertEquals(SSDOcr.OcrOutcome.Mismatch, prediction2.outcome)
    }
}

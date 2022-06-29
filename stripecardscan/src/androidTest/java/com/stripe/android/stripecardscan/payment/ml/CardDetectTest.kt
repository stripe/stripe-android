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
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CardDetectTest {
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
    fun cardDetect_pan() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.card_pan, null)
            .toBitmap()
            .scale(Size(224, 224))
        val fetcher = CardDetectModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = CardDetect.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            CardDetect.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )
        assertNotNull(prediction)
        assertEquals(CardDetect.Prediction.Side.PAN, prediction.side)
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
    fun cardDetect_noPan() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.card_no_pan, null)
            .toBitmap()
            .scale(Size(224, 224))
        val fetcher = CardDetectModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = CardDetect.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            CardDetect.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )
        assertNotNull(prediction)
        assertEquals(CardDetect.Prediction.Side.NO_PAN, prediction.side)
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
    fun cardDetect_noCard() = runBlocking {
        val bitmap = testContext.resources.getDrawable(R.drawable.card_no_card, null)
            .toBitmap()
            .scale(Size(224, 224))
        val fetcher = CardDetectModelManager.getModelFetcher(appContext)
        assertNotNull(fetcher)
        assertTrue(fetcher is ResourceFetcher)
        fetcher.clearCache()

        val model = CardDetect.Factory(
            appContext,
            fetcher.fetchData(forImmediateUse = true, isOptional = false)
        ).newInstance()
        assertNotNull(model)

        val prediction = model.analyze(
            CardDetect.cameraPreviewToInput(
                bitmap,
                bitmap.size().toRect(),
                bitmap.size().toRect()
            ),
            Unit
        )
        assertNotNull(prediction)
        assertEquals(CardDetect.Prediction.Side.NO_CARD, prediction.side)
    }
}

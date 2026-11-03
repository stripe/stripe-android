package com.stripe.android.stripecardscan.payment.ml

import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EmbossedModelDeviceTest {
    @Test
    fun compactModelRecognizesSyntheticEmbossedCard() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        Tasks.await(TfLite.initialize(context))
        val bitmap = instrumentation.context.assets.open("embossed_synthetic.jpg").use {
            BitmapFactory.decodeStream(it)
        }
        assertNotNull(bitmap)
        val fetchedModel = SSDOcrModelManager(SSDOcrModel.Compact).fetchModel(context, forImmediateUse = true)
        val model = SSDOcr.Factory(context, fetchedModel, SSDOcrModel.Compact).newInstance()
        assertNotNull(model, "The phone runtime must load the compact INT8-weight model")
        try {
            val bounds = Rect(0, 0, bitmap.width, bitmap.height)
            val input = CardOcr.cameraPreviewToInput(bitmap, bounds, bounds)
            repeat(3) {
                val prediction = model.analyze(input, Unit)
                assertEquals("4372265397654881", prediction.pan)
                assertEquals(CardOcr.Source.Darknite, prediction.source)
            }
        } finally {
            model.close()
            bitmap.recycle()
        }
    }
}

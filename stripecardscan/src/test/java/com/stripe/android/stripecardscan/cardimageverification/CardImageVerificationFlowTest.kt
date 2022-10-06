package com.stripe.android.stripecardscan.cardimageverification

import androidx.test.filters.SmallTest
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopAggregator
import com.stripe.android.stripecardscan.framework.util.AcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.util.OptionalImageSettings
import org.junit.Test
import kotlin.test.assertEquals

class CardImageVerificationFlowTest {

    @Test
    @SmallTest
    fun selectCompletionLoopFrames() {
        val flow = object : CardImageVerificationFlow(
            scanErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean = false
                override fun onResultFailure(t: Throwable): Boolean = false
            }
        ) {
            override suspend fun onInterimResult(result: MainLoopAggregator.InterimResult) {}
            override suspend fun onReset() {}
        }

        val frameMap = mapOf(
            SavedFrameType(hasCard = true, hasOcr = true) to listOf("A", "B"),
            SavedFrameType(hasCard = true, hasOcr = false) to listOf("D", "E"),
            SavedFrameType(hasCard = false, hasOcr = true) to listOf("G", "H"),
            SavedFrameType(hasCard = false, hasOcr = false) to listOf("J", "K")
        )

        val imageConfigs = AcceptedImageConfigs(
            defaultSettings = OptionalImageSettings(null, null, 5)
        )

        val selectedFrames = flow.selectCompletionLoopFrames(frameMap, imageConfigs)
        assertEquals(
            listOf("A", "B", "G", "H", "D"),
            selectedFrames
        )
    }
}

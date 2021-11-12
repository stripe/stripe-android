package com.stripe.android.cardverificationsheet.cardverifyui

import androidx.test.filters.SmallTest
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopAggregator
import com.stripe.android.cardverificationsheet.framework.AggregateResultListener
import com.stripe.android.cardverificationsheet.framework.AnalyzerLoopErrorListener
import org.junit.Test
import kotlin.test.assertEquals

class CardVerifyFlowTest {

    @Test
    @SmallTest
    fun selectCompletionLoopFrames() {

        val flow = CardVerifyFlow(
            scanResultListener = object :
                AggregateResultListener<
                    MainLoopAggregator.InterimResult,
                    MainLoopAggregator.FinalResult
                    > {
                override suspend fun onResult(result: MainLoopAggregator.FinalResult) {}
                override suspend fun onInterimResult(result: MainLoopAggregator.InterimResult) {}
                override suspend fun onReset() {}
            },
            scanErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean = false
                override fun onResultFailure(t: Throwable): Boolean = false
            }
        )

        val frameMap = mapOf(
            SavedFrameType(hasCard = true, hasOcr = true) to listOf("A", "B", "C"),
            SavedFrameType(hasCard = true, hasOcr = false) to listOf("D", "E", "F"),
            SavedFrameType(hasCard = false, hasOcr = true) to listOf("G", "H", "I"),
            SavedFrameType(hasCard = false, hasOcr = false) to listOf("J", "K", "L"),
        )

        val selectedFrames = flow.selectCompletionLoopFrames(frameMap)
        assertEquals(
            listOf("A", "B", "C", "G", "H"),
            selectedFrames,
        )
    }
}

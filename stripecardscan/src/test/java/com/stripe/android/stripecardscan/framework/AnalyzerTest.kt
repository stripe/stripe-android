package com.stripe.android.stripecardscan.framework

import androidx.test.filters.SmallTest
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.AnalyzerPool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AnalyzerTest {

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun analyzerPoolCreateNormally() = runTest {
        class TestAnalyzerFactory : AnalyzerFactory<Int, Int, Int, TestAnalyzer> {
            override suspend fun newInstance(): TestAnalyzer? = TestAnalyzer()
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        assertEquals(12, analyzerPool.desiredAnalyzerCount)
        assertEquals(12, analyzerPool.analyzers.size)
        assertEquals(3, analyzerPool.analyzers[0].analyze(1, 2))
    }

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun analyzerPoolCreateFailure() = runTest {
        class TestAnalyzerFactory : AnalyzerFactory<Int, Int, Int, TestAnalyzer> {
            override suspend fun newInstance(): TestAnalyzer? = null
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        assertEquals(12, analyzerPool.desiredAnalyzerCount)
        assertEquals(0, analyzerPool.analyzers.size)
    }

    private class TestAnalyzer : Analyzer<Int, Int, Int> {
        override suspend fun analyze(data: Int, state: Int): Int = data + state
    }
}

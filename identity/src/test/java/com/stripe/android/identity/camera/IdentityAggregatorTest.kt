package com.stripe.android.identity.camera

import android.graphics.Bitmap
import android.graphics.Rect
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.DocumentFrameCache
import com.stripe.android.identity.states.IDDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.ScanType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@RunWith(RobolectricTestRunner::class)
internal class IdentityAggregatorTest {
    @Test
    fun `timeout from initial uses even the very first blurry frame`() = runScenario {
        val first = output(blurScore = 0.3f)
        val started = aggregator.aggregateResult(input, first)
        assertThat(started.first.identityState).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat(started.second).isNull()
        expire(started.first.identityState)

        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.isTimeoutFallback).isTrue()
        assertThat(result.identityState).isInstanceOf(IdentityScanState.TimeOut::class.java)
        assertThat(result.frame).isSameInstanceAs(input)
        assertThat(result.result).isSameInstanceAs(first)
        assertThat(result.frame.cameraPreviewImage.exposureIso).isEqualTo(100f)
    }

    @Test
    fun `timeout from found uses earlier best blurry frame`() = runScenario {
        start()
        val best = output(blurScore = 0.3f)
        val found = scan(best).first.identityState
        scan(output(blurScore = 0.1f))
        expire(found)
        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.isTimeoutFallback).isTrue()
        assertThat(result.result).isSameInstanceAs(best)
    }

    @Test
    fun `timeout from unsatisfied uses earlier matching frame`() = runScenario {
        start()
        val best = output()
        scan(best)
        scan(output(category = Category.ID_BACK))
        val unsatisfied = scan(output(category = Category.ID_BACK)).first.identityState
        assertThat(unsatisfied).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        expire(unsatisfied)
        val result = requireNotNull(scan(output(category = Category.ID_BACK)).second)
        assertThat(result.isTimeoutFallback).isTrue()
        assertThat(result.result).isSameInstanceAs(best)
    }

    @Test
    fun `internal detection retry preserves fallback and deadline`() = runScenario {
        val transitioner = start()
        val deadline = transitioner.timeoutAt
        val best = output()
        scan(best)
        scan(output(category = Category.INVALID))
        scan(output(category = Category.INVALID))
        val restarted = scan(output(category = Category.INVALID)).first.identityState
        assertThat(restarted).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat(transitioner.timeoutAt).isSameInstanceAs(deadline)
        expire(restarted)
        assertThat(scan(output(category = Category.INVALID)).second?.result).isSameInstanceAs(best)
    }

    @Test
    fun `new scan attempt retains earlier passport fallback`() = runScenario {
        start()
        val best = output(category = Category.PASSPORT, blurScore = 0.4f)
        scan(best)
        aggregator = createAggregator(ScanType.DOC_FRONT)
        val transitioner = start()
        scan(output(category = Category.PASSPORT, blurScore = 0.1f))
        transitioner.timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        assertThat(scan(output(category = Category.INVALID)).second?.result).isSameInstanceAs(best)
    }

    @Test
    fun `back timeout cannot reuse front candidate`() = runScenario {
        start()
        scan(output())
        aggregator = createAggregator(ScanType.DOC_BACK)
        val transitioner = start()
        transitioner.timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.identityState).isInstanceOf(IdentityScanState.TimeOut::class.java)
        assertThat(result.isTimeoutFallback).isFalse()
    }

    @Test
    fun `back timeout selects back candidate independently of better front`() = runScenario {
        start()
        scan(output(blurScore = 0.5f, confidence = 1f))
        aggregator = createAggregator(ScanType.DOC_BACK)
        val transitioner = start()
        val back = output(category = Category.ID_BACK, blurScore = 0.1f)
        scan(back)
        transitioner.timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.isTimeoutFallback).isTrue()
        assertThat(result.result).isSameInstanceAs(back)
    }

    @Test
    fun `session reset discards previous fallback`() = runScenario {
        start()
        scan(output())
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNotNull()
        cache.reset()
        aggregator = createAggregator(ScanType.DOC_FRONT)
        val transitioner = start()
        transitioner.timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        assertThat(scan(output(category = Category.INVALID)).second?.isTimeoutFallback).isFalse()
    }

    @Test
    fun `timeout without matching document retains error result`() = runScenario {
        val transitioner = start()
        scan(output(category = Category.ID_BACK))
        transitioner.timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.isTimeoutFallback).isFalse()
        assertThat(result.identityState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `blurry fallback does not start normal capture window`() = runScenario {
        start()
        scan(output(blurScore = 0.4f))
        scan(output(blurScore = 0.4f))
        ShadowSystemClock.advanceBy(2000, TimeUnit.MILLISECONDS)
        val firstGood = scan(output(blurScore = 0.6f))
        assertThat(firstGood.first.identityState).isInstanceOf(IdentityScanState.Found::class.java)
        assertThat(firstGood.second).isNull()
        ShadowSystemClock.advanceBy(500, TimeUnit.MILLISECONDS)
        assertThat(scan(output(blurScore = 0.6f)).first.identityState)
            .isInstanceOf(IdentityScanState.Found::class.java)
    }

    @Test
    fun `normal capture keeps its scoring and small improvements with matching metadata`() = runScenario {
        start()
        val fallback = output(blurScore = 0.5f, confidence = 1f)
        scan(fallback)
        scan(output(blurScore = 0.6f, confidence = 0.2f))
        ShadowSystemClock.advanceBy(500, TimeUnit.MILLISECONDS)
        val bestInput = input.copy(cameraPreviewImage = input.cameraPreviewImage.copy(exposureIso = 200f))
        val best = output(blurScore = 0.61f, confidence = 0.2f)
        aggregator.aggregateResult(bestInput, best)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(fallback)
        ShadowSystemClock.advanceBy(501, TimeUnit.MILLISECONDS)
        assertThat(scan(output(category = Category.INVALID)).first.identityState)
            .isInstanceOf(IdentityScanState.Satisfied::class.java)
        val result = requireNotNull(scan(output(category = Category.INVALID)).second)
        assertThat(result.identityState).isInstanceOf(IdentityScanState.Finished::class.java)
        assertThat(result.isTimeoutFallback).isFalse()
        assertThat(result.result).isSameInstanceAs(best)
        assertThat(result.frame).isSameInstanceAs(bestInput)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val listener = FakeAggregateResultListener()
        Scenario(listener).block()
        listener.ensureAllEventsConsumed()
    }

    private class Scenario(private val listener: FakeAggregateResultListener) {
        val cache = DocumentFrameCache()
        val input = AnalyzerInput(
            CameraPreviewImage(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), Rect(), exposureIso = 100f),
            Rect()
        )
        var aggregator = createAggregator(ScanType.DOC_FRONT)

        fun createAggregator(scanType: ScanType) = IdentityAggregator(
            scanType,
            listener,
            SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE.let {
                it.copy(documentCapture = it.documentCapture.copy(blurThreshold = 0.5f))
            },
            cache
        )

        suspend fun start(): IDDetectorTransitioner {
            val state = scan(output(category = Category.INVALID)).first.identityState
            return (state.transitioner as IDDetectorTransitioner).also {
                it.timeoutAt = TimeSource.Monotonic.markNow() + 1.hours
            }
        }

        suspend fun scan(output: IDDetectorOutput) = aggregator.aggregateResult(input, output)

        fun expire(state: IdentityScanState) {
            (state.transitioner as IDDetectorTransitioner).timeoutAt = TimeSource.Monotonic.markNow() - 1.seconds
        }

        fun output(
            category: Category = Category.ID_FRONT,
            blurScore: Float = 0.2f,
            confidence: Float = 0.8f
        ) = IDDetectorOutput(
            BoundingBox(0.25f, 0.25f, 0.5f, 0.5f), category, confidence,
            listOf(0f, confidence, 0f, 0f), blurScore, input.cameraPreviewImage.image
        )
    }

    private class FakeAggregateResultListener :
        AggregateResultListener<IdentityAggregator.InterimResult, IdentityAggregator.FinalResult> {
        private val results = Turbine<IdentityAggregator.FinalResult>()
        private val interimResults = Turbine<IdentityAggregator.InterimResult>()
        private val resets = Turbine<Unit>()

        override suspend fun onResult(result: IdentityAggregator.FinalResult) = results.add(result)
        override suspend fun onInterimResult(result: IdentityAggregator.InterimResult) = interimResults.add(result)
        override suspend fun onReset() = resets.add(Unit)

        fun ensureAllEventsConsumed() {
            results.ensureAllEventsConsumed()
            interimResults.ensureAllEventsConsumed()
            resets.ensureAllEventsConsumed()
        }
    }
}

package com.stripe.android.identity.states

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState.ScanType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DocumentFrameCacheTest {
    @Test
    fun `keeps the best blurry frame together with its metadata`() = runScenario {
        val best = output(blurScore = 0.3f)
        cache.consider(input, best, ScanType.DOC_FRONT)
        cache.consider(
            input.copy(cameraPreviewImage = input.cameraPreviewImage.copy(exposureIso = 200f)),
            output(blurScore = 0.1f),
            ScanType.DOC_FRONT
        )

        val candidate = requireNotNull(cache.bestCandidate(ScanType.DOC_FRONT))
        assertThat(candidate.input).isSameInstanceAs(input)
        assertThat(candidate.output).isSameInstanceAs(best)
        assertThat(candidate.input.cameraPreviewImage.exposureIso).isEqualTo(100f)
    }

    @Test
    fun `score equally weights normalized sharpness coverage and confidence`() = runScenario {
        cache.consider(input, output(blurScore = 0.3f, confidence = 0.8f), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.score).isWithin(0.00001f).of(0.45f)
    }

    @Test
    fun `clamps sharpness and confidence to their normalized range`() = runScenario {
        cache.consider(input, output(blurScore = 2f, confidence = -1f), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.score).isWithin(0.00001f).of(1.25f / 3f)
    }

    @Test
    fun `small score improvement does not replace cached frame`() = runScenario {
        val first = output(blurScore = 0f, confidence = 0.5f)
        cache.consider(input, first, ScanType.DOC_FRONT)
        cache.consider(input, output(blurScore = 0f, confidence = 0.559997f), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(first)
    }

    @Test
    fun `exactly two hundredths improvement replaces cached frame`() = runScenario {
        cache.consider(input, output(blurScore = 0f, confidence = 0.5f), ScanType.DOC_FRONT)
        val better = output(blurScore = 0f, confidence = 0.56f)
        cache.consider(input, better, ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(better)
    }

    @Test
    fun `improvement is measured against latest cached frame`() = runScenario {
        cache.consider(input, output(blurScore = 0f, confidence = 0.5f), ScanType.DOC_FRONT)
        val better = output(blurScore = 0f, confidence = 0.65f)
        cache.consider(input, better, ScanType.DOC_FRONT)
        cache.consider(input, output(blurScore = 0f, confidence = 0.68f), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(better)
    }

    @Test
    fun `keeps front and back candidates separate`() = runScenario {
        val front = output(blurScore = 0.8f)
        val back = output(category = Category.ID_BACK, blurScore = 0.1f)
        cache.consider(input, front, ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_BACK)).isNull()
        cache.consider(input, back, ScanType.DOC_BACK)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(front)
        assertThat(cache.bestCandidate(ScanType.DOC_BACK)?.output).isSameInstanceAs(back)
    }

    @Test
    fun `passport is eligible for front capture`() = runScenario {
        val passport = output(category = Category.PASSPORT)
        cache.consider(input, passport, ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(passport)
    }

    @Test
    fun `wrong side cannot become fallback`() = runScenario {
        cache.consider(input, output(category = Category.ID_BACK), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNull()
    }

    @Test
    fun `passport cannot become back fallback`() = runScenario {
        cache.consider(input, output(category = Category.PASSPORT), ScanType.DOC_BACK)
        assertThat(cache.bestCandidate(ScanType.DOC_BACK)).isNull()
    }

    @Test
    fun `invalid document cannot become fallback`() = runScenario {
        cache.consider(input, output(category = Category.INVALID), ScanType.DOC_FRONT)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNull()
    }

    @Test
    fun `selfie has no document fallback`() = runScenario {
        cache.consider(input, output(), ScanType.SELFIE)
        assertThat(cache.bestCandidate(ScanType.SELFIE)).isNull()
    }

    @Test
    fun `empty bounds do not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(0f, 0f, 0f, 0f)))
    }

    @Test
    fun `negative dimensions do not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(0.25f, 0.25f, -0.5f, 0.5f)))
    }

    @Test
    fun `bounds beyond frame do not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(0.6f, 0.25f, 0.5f, 0.5f)))
    }

    @Test
    fun `negative origin does not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(-0.1f, 0.25f, 0.5f, 0.5f)))
    }

    @Test
    fun `nonfinite origin does not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(Float.NaN, 0.25f, 0.5f, 0.5f)))
    }

    @Test
    fun `infinite bounds do not replace cached frame`() = runScenario {
        assertRejected(output(bounds = BoundingBox(0f, 0f, Float.POSITIVE_INFINITY, 0.5f)))
    }

    @Test
    fun `nonfinite confidence does not replace cached frame`() = runScenario {
        assertRejected(output(confidence = Float.NaN))
    }

    @Test
    fun `nonfinite sharpness does not replace cached frame`() = runScenario {
        assertRejected(output(blurScore = Float.POSITIVE_INFINITY))
    }

    @Test
    fun `reset clears both cached sides`() = runScenario {
        cache.consider(input, output(), ScanType.DOC_FRONT)
        cache.consider(input, output(category = Category.ID_BACK), ScanType.DOC_BACK)
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNotNull()
        assertThat(cache.bestCandidate(ScanType.DOC_BACK)).isNotNull()
        cache.reset()
        assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNull()
        assertThat(cache.bestCandidate(ScanType.DOC_BACK)).isNull()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        Scenario(
            DocumentFrameCache(),
            AnalyzerInput(CameraPreviewImage(bitmap, Rect(0, 0, 100, 100), exposureIso = 100f), Rect())
        ).block()
    }

    private class Scenario(val cache: DocumentFrameCache, val input: AnalyzerInput) {
        fun output(
            category: Category = Category.ID_FRONT,
            blurScore: Float = 0.2f,
            confidence: Float = 0.8f,
            bounds: BoundingBox = BoundingBox(0.25f, 0.25f, 0.5f, 0.5f)
        ) = IDDetectorOutput(
            bounds, category, confidence, listOf(0f, confidence, 0f, 0f),
            blurScore, input.cameraPreviewImage.image
        )

        fun assertRejected(candidate: IDDetectorOutput) {
            cache.consider(input, candidate, ScanType.DOC_FRONT)
            assertThat(cache.bestCandidate(ScanType.DOC_FRONT)).isNull()
            val first = output(blurScore = 0f, confidence = 0f)
            cache.consider(input, first, ScanType.DOC_FRONT)
            cache.consider(input, candidate, ScanType.DOC_FRONT)
            assertThat(cache.bestCandidate(ScanType.DOC_FRONT)?.output).isSameInstanceAs(first)
        }
    }
}

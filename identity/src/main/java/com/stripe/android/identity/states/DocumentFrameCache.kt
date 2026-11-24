package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState.ScanType

/** Retains a timeout fallback per document side, independently of the successful capture window. */
internal class DocumentFrameCache {
    private val candidates = mutableMapOf<ScanType, Candidate>()

    fun consider(input: AnalyzerInput, output: IDDetectorOutput, scanType: ScanType) {
        val matchesSide = when (scanType) {
            ScanType.DOC_FRONT -> output.category == Category.ID_FRONT || output.category == Category.PASSPORT
            ScanType.DOC_BACK -> output.category == Category.ID_BACK
            ScanType.SELFIE -> false
        }
        val bounds = output.boundingBox
        if (!matchesSide || !bounds.isValid() ||
            !output.blurScore.isFinite() || !output.resultScore.isFinite()
        ) {
            return
        }

        // Android's Laplacian sharpness metric is already normalized to 0...1.
        val score = (
            output.blurScore.coerceIn(0f, 1f) +
                (bounds.width * bounds.height).coerceIn(0f, 1f) +
                output.resultScore.coerceIn(0f, 1f)
            ) / SCORE_COMPONENT_COUNT
        val current = candidates[scanType]
        // Allow one rounding step for improvements of exactly 0.02.
        if (current == null || score >= Math.nextDown(current.score + MINIMUM_SCORE_IMPROVEMENT)) {
            candidates[scanType] = Candidate(input, output, score)
        }
    }

    fun bestCandidate(scanType: ScanType): Candidate? = candidates[scanType]

    fun reset() {
        candidates.clear()
    }

    @Suppress("ComplexCondition")
    private fun BoundingBox.isValid(): Boolean =
        left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite() &&
            width > 0f && height > 0f && left >= 0f && top >= 0f &&
            left + width <= 1f && top + height <= 1f

    internal data class Candidate(
        val input: AnalyzerInput,
        val output: IDDetectorOutput,
        val score: Float
    )

    private companion object {
        const val MINIMUM_SCORE_IMPROVEMENT = 0.02f
        const val SCORE_COMPONENT_COUNT = 3f
    }
}

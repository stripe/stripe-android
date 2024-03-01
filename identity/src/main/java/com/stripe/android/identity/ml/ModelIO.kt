package com.stripe.android.identity.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.states.MBDetector

/**
 * Result category of IDDetector
 */
internal enum class Category { NO_ID, PASSPORT, ID_FRONT, ID_BACK, INVALID }

/**
 * Result bounding box coordinates of IDDetector, in percentage values with regard to original image's width/height
 */
internal data class BoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Input from CameraAdapter, note: the bitmap should already be encoded in RGB value
 */
internal data class AnalyzerInput(
    val cameraPreviewImage: CameraPreviewImage<Bitmap>,
    val viewFinderBounds: Rect
)

/**
 * Output interface of ML models
 */
internal sealed interface AnalyzerOutput

/**
 * Output of IDDetector
 */
internal sealed class IDDetectorOutput(
    val category: Category,
    val resultScore: Float,
    val allScores: List<Float>,
) : AnalyzerOutput {
    class Legacy(
        val boundingBox: BoundingBox,
        category: Category,
        resultScore: Float,
        allScores: List<Float>,
        val blurScore: Float
    ) : IDDetectorOutput(category, resultScore, allScores)

    class Modern(
        category: Category,
        resultScore: Float,
        allScores: List<Float>,
        val mbOutput: MBDetector.DetectorResult
    ) : IDDetectorOutput(category, resultScore, allScores)

    fun blurScore(): Float? =
        when (this) {
            is Legacy -> {
                this.blurScore
            }
            is Modern -> {
                null
            }
        }
}

/**
 * Output of FaceDetector
 */
internal data class FaceDetectorOutput(
    val boundingBox: BoundingBox,
    val resultScore: Float
) : AnalyzerOutput

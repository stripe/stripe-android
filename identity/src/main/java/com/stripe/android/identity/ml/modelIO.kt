package com.stripe.android.identity.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.stripe.android.camera.CameraPreviewImage

/**
 * Result category of IDDetector
 */
internal enum class Category { NO_ID, PASSPORT, ID_FRONT, ID_BACK, INVALID }

/**
 * Result bounding box coordinates of IDDetector, in percentage values with regard to original image's width/height
 */
internal data class BoundingBox(
    val top: Float,
    val left: Float,
    val width: Float,
    val height: Float,
)

/**
 * Input from CameraAdapter, note: the bitmap should already be encoded in RGB value
 */
internal data class AnalyzerInput(
    val cameraPreviewImage: CameraPreviewImage<Bitmap>,
    val viewFinderBounds: Rect
)

/**
 * Output the category with highest score and the bounding box
 */
internal data class AnalyzerOutput(
    val boundingBox: BoundingBox,
    val category: Category,
    val score: Float
)

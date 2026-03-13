package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import androidx.annotation.CheckResult

/**
 * Convert a [Bitmap] to an [MLImage] for use in ML models.
 */
@CheckResult
internal fun Bitmap.toMLImage(mean: Float = 0F, std: Float = 255F) = MLImage(this, mean, std)

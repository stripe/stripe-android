package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.annotation.CheckResult
import com.stripe.android.stripecardscan.framework.util.centerOn
import com.stripe.android.stripecardscan.framework.util.intersectionWith
import com.stripe.android.stripecardscan.framework.util.move
import com.stripe.android.stripecardscan.framework.util.resizeRegion
import com.stripe.android.stripecardscan.framework.util.scaleAndCenterWithin
import com.stripe.android.stripecardscan.framework.util.size
import com.stripe.android.stripecardscan.framework.util.toRect
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

@CheckResult
internal fun Bitmap.toWebP(): ByteArray =
    ByteArrayOutputStream().use {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.compress(Bitmap.CompressFormat.WEBP_LOSSY, 92, it)
        } else {
            @Suppress("deprecation")
            this.compress(Bitmap.CompressFormat.WEBP, 92, it)
        }
        it.flush()
        it.toByteArray()
    }

@CheckResult
internal fun Bitmap.toJpeg(): ByteArray =
    ByteArrayOutputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 92, it)
        it.flush()
        it.toByteArray()
    }

/**
 * Crop a [Bitmap] to a given [Rect]. The crop must have a positive area and must be contained
 * within the bounds of the source [Bitmap].
 */
@CheckResult
internal fun Bitmap.crop(crop: Rect): Bitmap {
    require(crop.left < crop.right && crop.top < crop.bottom) { "Cannot use negative crop" }
    require(
        crop.left >= 0 &&
            crop.top >= 0 &&
            crop.bottom <= this.height &&
            crop.right <= this.width
    ) {
        "Crop is larger than source image"
    }
    return Bitmap.createBitmap(this, crop.left, crop.top, crop.width(), crop.height())
}

@CheckResult
internal fun Bitmap.cropCenter(size: Size): Bitmap =
    if (size.width > width || size.height > height) {
        val cropRegion = size.scaleAndCenterWithin(Size(width, height))
        crop(cropRegion).scale(size)
    } else {
        crop(size.centerOn(size().toRect()))
    }

/**
 * Scale a [Bitmap] by a given [percentage].
 */
@CheckResult
internal fun Bitmap.scale(percentage: Float, filter: Boolean = false): Bitmap =
    if (percentage == 1F) {
        this
    } else {
        Bitmap.createScaledBitmap(
            this,
            (width * percentage).toInt(),
            (height * percentage).toInt(),
            filter
        )
    }

/**
 * Get the size of a [Bitmap].
 */
@CheckResult
internal fun Bitmap.size() = Size(this.width, this.height)

/**
 * Scale the [Bitmap] to circumscribe the given [Size], then crop the excess.
 */
@CheckResult
internal fun Bitmap.scaleAndCrop(size: Size, filter: Boolean = false): Bitmap =
    if (size.width == width && size.height == height) {
        this
    } else {
        val scaleFactor = max(
            size.width.toFloat() / this.width,
            size.height.toFloat() / this.height,
        )
        val scaled = this.scale(scaleFactor, filter)
        scaled.crop(size.centerOn(scaled.size().toRect()))
    }

/**
 * Crops and image using originalImageRect and places it on finalImageRect, which is filled with
 * gray for the best results
 */
@CheckResult
internal fun Bitmap.cropWithFill(cropRegion: Rect): Bitmap {
    val intersectionRegion = this.size().toRect().intersectionWith(cropRegion)
    val result = Bitmap.createBitmap(cropRegion.width(), cropRegion.height(), this.config)
    val canvas = Canvas(result)

    canvas.drawColor(Color.GRAY)

    val croppedImage = this.crop(intersectionRegion)

    canvas.drawBitmap(
        croppedImage,
        croppedImage.size().toRect(),
        intersectionRegion.move(-cropRegion.left, -cropRegion.top),
        null
    )

    return result
}

/**
 * Fragments the [Bitmap] into multiple segments and places them in new segments.
 */
@CheckResult
internal fun Bitmap.rearrangeBySegments(
    segmentMap: Map<Rect, Rect>
): Bitmap {
    if (segmentMap.isEmpty()) {
        return Bitmap.createBitmap(0, 0, this.config)
    }
    val newImageDimensions = segmentMap.values.reduce { a, b ->
        Rect(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )
    }
    val newImageSize = newImageDimensions.size()
    val result = Bitmap.createBitmap(newImageSize.width, newImageSize.height, this.config)
    val canvas = Canvas(result)

    // This should be using segmentMap.forEach, but doing so seems to require API 24. It's unclear
    // why this won't use the kotlin.collections version of `forEach`, but it's not during compile.
    for (it in segmentMap) {
        val from = it.key
        val to = it.value.move(-newImageDimensions.left, -newImageDimensions.top)

        val segment = this.crop(from).scale(to.size())
        canvas.drawBitmap(
            segment,
            to.left.toFloat(),
            to.top.toFloat(),
            null
        )
    }

    return result
}

/**
 * Selects a region from the source [Bitmap], resizing that to a new region, and transforms the
 * remainder of the [Bitmap] into a border. See [resizeRegion] and [rearrangeBySegments].
 */
@CheckResult
internal fun Bitmap.zoom(
    originalRegion: Rect,
    newRegion: Rect,
    newImageSize: Size
): Bitmap {
    // Creates a map of rects->rects which map segments of the old image onto the new one
    val regionMap = this.size().resizeRegion(originalRegion, newRegion, newImageSize)
    // construct the bitmap from the region map
    return this.rearrangeBySegments(regionMap)
}

internal fun Bitmap.scale(size: Size, filter: Boolean = false): Bitmap =
    if (size.width == width && size.height == height) {
        this
    } else {
        Bitmap.createScaledBitmap(this, size.width, size.height, filter)
    }

/**
 * Constrain a bitmap to a given size, while maintaining its original aspect ratio.
 */
internal fun Bitmap.constrainToSize(size: Size, filter: Boolean = false): Bitmap =
    if (size.width >= width && size.height >= height) {
        this
    } else {
        val newSize = this.size().scaleAndCenterWithin(size).size()
        Bitmap.createScaledBitmap(this, newSize.width, newSize.height, filter)
    }

/**
 * Convert a [Bitmap] to an [MLImage] for use in ML models.
 */
@CheckResult
internal fun Bitmap.toMLImage(mean: Float = 0F, std: Float = 255F) = MLImage(this, mean, std)

/**
 * Convert a [Bitmap] to an [MLImage] for use in ML models.
 */
@CheckResult
internal fun Bitmap.toMLImage(mean: ImageTransformValues, std: ImageTransformValues) =
    MLImage(this, mean, std)

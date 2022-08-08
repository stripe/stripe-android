package com.stripe.android.camera.framework.util

import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import android.util.SizeF
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Determine the maximum size of rectangle with a given aspect ratio (X/Y) that can fit inside the
 * specified area.
 *
 * For example, if the aspect ratio is 1/2 and the area is 2x2, the resulting rectangle would be
 * size 1x2 and look like this:
 * ```
 *  ________
 * | |    | |
 * | |    | |
 * | |    | |
 * |_|____|_|
 * ```
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun maxAspectRatioInSize(area: Size, aspectRatio: Float): Size {
    var width = area.width
    var height = (width / aspectRatio).roundToInt()

    return if (height <= area.height) {
        Size(area.width, height)
    } else {
        height = area.height
        width = (height * aspectRatio).roundToInt()
        Size(min(width, area.width), height)
    }
}

/**
 * Determine the minimum size of rectangle with a given aspect ratio (X/Y) that a specified area
 * can fit inside.
 *
 * For example, if the aspect ratio is 1/2 and the area is 1x1, the resulting rectangle would be
 * size 1x2 and look like this:
 * ```
 *  ____
 * |____|
 * |    |
 * |____|
 * |____|
 * ```
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun minAspectRatioSurroundingSize(area: Size, aspectRatio: Float): Size {
    var width = area.width
    var height = (width / aspectRatio).roundToInt()

    return if (height >= area.height) {
        Size(area.width, height)
    } else {
        height = area.height
        width = (height * aspectRatio).roundToInt()
        Size(max(width, area.width), height)
    }
}

/**
 * Given a size and an aspect ratio, resize the area to fit that aspect ratio. If the desired aspect
 * ratio is smaller than the one of the provided size, the size will be cropped to match. If the
 * desired aspect ratio is larger than the that of the provided size, then the size will be expanded
 * to match.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun adjustSizeToAspectRatio(area: Size, aspectRatio: Float): Size = if (aspectRatio < 1) {
    Size(area.width, (area.width / aspectRatio).roundToInt())
} else {
    Size((area.height * aspectRatio).roundToInt(), area.height)
}

/**
 * Scale up a [Size] so that it fills a [containingSize] while maintaining its original aspect
 * ratio.
 *
 * If using this to project a preview image onto a full camera image, This makes a few assumptions:
 * 1. the preview image [Size] and full image [containingSize] are centered relative to each other
 * 2. the preview image and the full image have the same orientation
 * 3. the preview image and the full image share either a horizontal or vertical field of view
 * 4. the non-shared field of view must be smaller on the preview image than the full image
 *
 * Note that the [Size] and the [containingSize] are allowed to have completely independent
 * resolutions.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scaleAndCenterWithin(containingSize: Size): Rect {
    val aspectRatio = width.toFloat() / height

    // Since the preview image may be at a different resolution than the full image, scale the
    // preview image to be circumscribed by the fullImage.
    val scaledSize = maxAspectRatioInSize(containingSize, aspectRatio)
    val left = (containingSize.width - scaledSize.width) / 2
    val top = (containingSize.height - scaledSize.height) / 2
    return Rect(
        left,
        top,
        left + scaledSize.width,
        top + scaledSize.height
    )
}

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scaleAndCenterWithin(containingRect: Rect): Rect =
    this.scaleAndCenterWithin(containingRect.size()).move(containingRect.left, containingRect.top)

/**
 * Calculate the position of the [Size] surrounding the [surroundedSize]. This makes a few
 * assumptions:
 * 1. the [Size] and the [surroundedSize] are centered relative to each other.
 * 2. the [Size] and the [surroundedSize] have the same orientation
 * 3. the [surroundedSize] and the [Size] share either a horizontal or vertical field of view
 * 4. the non-shared field of view must be smaller on the [surroundedSize] than the [Size]
 *
 * If using this to project a full camera image onto a preview image, This makes a few assumptions:
 * 1. the preview image [surroundedSize] and full image [Size] are centered relative to each other
 * 2. the preview image and the full image have the same orientation
 * 3. the preview image and the full image share either a horizontal or vertical field of view
 * 4. the non-shared field of view must be smaller on the preview image than the full image
 *
 * Note that the [Size] and the [surroundedSize] are allowed to have completely independent
 * resolutions.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scaleAndCenterSurrounding(surroundedSize: Size): Rect {
    val aspectRatio = width.toFloat() / height

    val scaledSize = minAspectRatioSurroundingSize(surroundedSize, aspectRatio)
    val left = (surroundedSize.width - scaledSize.width) / 2
    val top = (surroundedSize.height - scaledSize.height) / 2
    return Rect(
        left,
        top,
        left + scaledSize.width,
        top + scaledSize.height
    )
}

/**
 * Scale a size based on percentage scale values, and keep track of its position.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scaleCentered(x: Float, y: Float): Rect {
    val newSize = this.scale(x, y)
    val left = (this.width - newSize.width) / 2
    val top = (this.height - newSize.height) / 2
    return Rect(
        left,
        top,
        left + newSize.width,
        top + newSize.height
    )
}

/**
 * Calculate the new size based on percentage scale values.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SizeF.scale(x: Float, y: Float) = SizeF(this.width * x, this.height * y)

/**
 * Calculate the new size based on a percentage scale.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SizeF.scale(scale: Float) = this.scale(scale, scale)

/**
 * Calculate the new size based on percentage scale values.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scale(x: Float, y: Float): Size =
    Size((this.width * x).roundToInt(), (this.height * y).roundToInt())

/**
 * Calculate the new size based on a percentage scale.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.scale(scale: Float) = this.scale(scale, scale)

/**
 * Center a size on a given rectangle. The size may be larger or smaller than the rect.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.centerOn(rect: Rect) = Rect(
    /* left */
    rect.centerX() - this.width / 2,
    /* top */
    rect.centerY() - this.height / 2,
    /* right */
    rect.centerX() + this.width / 2,
    /* bottom */
    rect.centerY() + this.height / 2
)

/**
 * Scale a [Rect] to have a size equivalent to the [scaledSize]. This will also scale the position
 * of the [Rect].
 *
 * For example, scaling a Rect(1, 2, 3, 4) by Size(5, 6) will result in a Rect(5, 12, 15, 24)
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.scaled(scaledSize: Size) = RectF(
    this.left * scaledSize.width,
    this.top * scaledSize.height,
    this.right * scaledSize.width,
    this.bottom * scaledSize.height
)

/**
 * Scale a [Rect] to have a size equivalent to the [scaledSize]. This will maintain the center
 * position of the [Rect].
 *
 * For example, scaling a Rect(5, 6, 7, 8) by Size(2, 0.5) will result
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.centerScaled(scaleX: Float, scaleY: Float) = RectF(
    this.centerX() - this.width() * scaleX / 2,
    this.centerY() - this.height() * scaleY / 2,
    this.centerX() + this.width() * scaleX / 2,
    this.centerY() + this.height() * scaleY / 2
)

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.centerScaled(scaleX: Float, scaleY: Float) = Rect(
    this.centerX() - (this.width() * scaleX / 2).toInt(),
    this.centerY() - (this.height() * scaleY / 2).toInt(),
    this.centerX() + (this.width() * scaleX / 2).toInt(),
    this.centerY() + (this.height() * scaleY / 2).toInt()
)

/**
 * Converts a size to rectangle with the top left corner at 0,0
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.toRect() = Rect(0, 0, this.width, this.height)

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.toRectF() = RectF(0F, 0F, this.width.toFloat(), this.height.toFloat())

/**
 * Transpose a size's width and height.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.transpose() = Size(this.height, this.width)

/**
 * Return a rect that is the intersection of two other rects
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.intersectionWith(rect: Rect): Rect {
    require(this.intersect(rect)) {
        "Given rects do not intersect $this <> $rect"
    }

    return Rect(
        max(this.left, rect.left),
        max(this.top, rect.top),
        min(this.right, rect.right),
        min(this.bottom, rect.bottom)
    )
}

/**
 * Move relative to its current position
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.move(relativeX: Int, relativeY: Int) = Rect(
    this.left + relativeX,
    this.top + relativeY,
    this.right + relativeX,
    this.bottom + relativeY
)

/**
 * Move relative to its current position
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.move(relativeX: Float, relativeY: Float) = RectF(
    this.left + relativeX,
    this.top + relativeY,
    this.right + relativeX,
    this.bottom + relativeY
)

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.toSizeF() = SizeF(width.toFloat(), height.toFloat())

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SizeF.toSize() = Size(width.roundToInt(), height.roundToInt())

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.toRectF() =
    RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.toRect() =
    Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

/**
 * Takes a relation between a region of interest and a size and projects the region of interest
 * to that new location
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SizeF.projectRegionOfInterest(toSize: SizeF, regionOfInterest: RectF): RectF {
    require(this.width > 0 && this.height > 0) {
        "Cannot project from container with non-positive dimensions"
    }

    return RectF(
        regionOfInterest.left * toSize.width / this.width,
        regionOfInterest.top * toSize.height / this.height,
        regionOfInterest.right * toSize.width / this.width,
        regionOfInterest.bottom * toSize.height / this.height
    )
}

/**
 * Takes a relation between a region of interest and a size and projects the region of interest
 * to that new location
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.projectRegionOfInterest(toSize: Size, regionOfInterest: Rect) =
    this.toSizeF().projectRegionOfInterest(toSize.toSizeF(), regionOfInterest.toRectF()).toRect()

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.projectRegionOfInterest(toSize: SizeF, regionOfInterest: RectF) =
    this.size().projectRegionOfInterest(toSize, regionOfInterest.move(-this.left, -this.top))

@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.projectRegionOfInterest(toSize: Size, regionOfInterest: Rect) =
    this.size().projectRegionOfInterest(toSize, regionOfInterest.move(-this.left, -this.top))

/**
 * Project a region of interest from one [Rect] to another. For example, given the rect and region
 * of interest:
 *  _______
 * |       |
 * |    _  |
 * |   |_| |
 * |       |
 * |_______|
 *
 * When projected to the following region:
 *  ___________
 * |           |
 * |           |
 * |___________|
 *
 * The position and size of the region of interest are scaled to the new rect.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.projectRegionOfInterest(toRect: RectF, regionOfInterest: RectF) =
    this.projectRegionOfInterest(toRect.size(), regionOfInterest).move(toRect.left, toRect.top)

/**
 * Project a region of interest from one [Rect] to another. For example, given the rect and region
 * of interest:
 *  _______
 * |       |
 * |    _  |
 * |   |_| |
 * |       |
 * |_______|
 *
 * When projected to the following region:
 *  ___________
 * |           |
 * |           |
 * |___________|
 *
 * The position and size of the region of interest are scaled to the new rect.
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.projectRegionOfInterest(toRect: Rect, regionOfInterest: Rect) =
    this.projectRegionOfInterest(toRect.size(), regionOfInterest).move(toRect.left, toRect.top)

/**
 * This method allows relocating and resizing a portion of a [Size]. It returns the required
 * translations required to achieve this relocation. This is useful for zooming in on sections of
 * an image.
 *
 * For example, given a size 5x5 and an original region (2, 2, 3, 3):
 *
 *  _______
 * |       |
 * |   _   |
 * |  |_|  |
 * |       |
 * |_______|
 *
 * If the [newRegion] is (1, 1, 4, 4) and the [newSize] is 6x6, the result will look like this:
 *
 *  ________
 * |  ___   |
 * | |   |  |
 * | |   |  |
 * | |___|  |
 * |        |
 * |________|
 *
 * Nine individual translations will be returned for the affected regions. The returned [Rect]s
 * will look like this:
 *
 *  ________
 * |_|___|__|
 * | |   |  |
 * | |   |  |
 * |_|___|__|
 * | |   |  |
 * |_|___|__|
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.resizeRegion(
    originalRegion: Rect,
    newRegion: Rect,
    newSize: Size
): Map<Rect, Rect> = mapOf(
    Rect(
        0,
        0,
        originalRegion.left,
        originalRegion.top
    ) to Rect(
        0,
        0,
        newRegion.left,
        newRegion.top
    ),
    Rect(
        originalRegion.left,
        0,
        originalRegion.right,
        originalRegion.top
    ) to Rect(
        newRegion.left,
        0,
        newRegion.right,
        newRegion.top
    ),
    Rect(
        originalRegion.right,
        0,
        this.width,
        originalRegion.top
    ) to Rect(
        newRegion.right,
        0,
        newSize.width,
        newRegion.top
    ),
    Rect(
        0,
        originalRegion.top,
        originalRegion.left,
        originalRegion.bottom
    ) to Rect(
        0,
        newRegion.top,
        newRegion.left,
        newRegion.bottom
    ),
    Rect(
        originalRegion.left,
        originalRegion.top,
        originalRegion.right,
        originalRegion.bottom
    ) to Rect(
        newRegion.left,
        newRegion.top,
        newRegion.right,
        newRegion.bottom
    ),
    Rect(
        originalRegion.right,
        originalRegion.top,
        this.width,
        originalRegion.bottom
    ) to Rect(
        newRegion.right,
        newRegion.top,
        newSize.width,
        newRegion.bottom
    ),
    Rect(
        0,
        originalRegion.bottom,
        originalRegion.left,
        this.height
    ) to Rect(
        0,
        newRegion.bottom,
        newRegion.left,
        newSize.height
    ),
    Rect(
        originalRegion.left,
        originalRegion.bottom,
        originalRegion.right,
        this.height
    ) to Rect(
        newRegion.left,
        newRegion.bottom,
        newRegion.right,
        newSize.height
    ),
    Rect(
        originalRegion.right,
        originalRegion.bottom,
        this.width,
        this.height
    ) to Rect(
        newRegion.right,
        newRegion.bottom,
        newSize.width,
        newSize.height
    )
)

/**
 * Determine the size of a [Rect].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Rect.size() = Size(width(), height())

/**
 * Determine the size of a [RectF].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun RectF.size() = SizeF(width(), height())

/**
 * Determine the aspect ratio of a [Size].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Size.aspectRatio() = width.toFloat() / height.toFloat()

/**
 * Determine the aspect ratio of a [SizeF].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SizeF.aspectRatio() = width / height

/**
 * Determine the size of a [View].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun View.size() = Size(width, height)

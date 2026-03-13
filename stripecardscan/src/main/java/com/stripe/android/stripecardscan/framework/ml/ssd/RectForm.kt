package com.stripe.android.stripecardscan.framework.ml.ssd

import android.graphics.RectF
import com.stripe.android.stripecardscan.framework.util.clamp

/**
 * An array of four floats, which denote a rectangle of the following values:
 * [0] = left percent
 * [1] = top percent
 * [2] = right percent
 * [3] = bottom percent
 */
internal typealias RectForm = FloatArray

internal const val RECT_FORM_SIZE = 4

/**
 * Create a new [RectForm].
 */
internal fun rectForm(left: Float, top: Float, right: Float, bottom: Float) =
    RectForm(RECT_FORM_SIZE).apply {
        setLeft(left)
        setTop(top)
        setRight(right)
        setBottom(bottom)
    }

internal fun RectForm.left() = this[0]
internal fun RectForm.top() = this[1]
internal fun RectForm.right() = this[2]
internal fun RectForm.bottom() = this[3]

internal fun RectForm.setLeft(left: Float) { this[0] = left }
internal fun RectForm.setTop(top: Float) { this[1] = top }
internal fun RectForm.setRight(right: Float) { this[2] = right }
internal fun RectForm.setBottom(bottom: Float) { this[3] = bottom }

internal fun RectForm.calcWidth() = right() - left()
internal fun RectForm.calcHeight() = bottom() - top()

/**
 * Convert this [RectForm] to a [RectF].
 */
internal fun RectForm.toRectF() = RectF(left(), top(), right(), bottom())

/**
 * Calculate the area of a rectangle while clamping the width and height between 0 and 1000.
 */
internal fun RectForm.areaClamped() = clamp(calcWidth(), 0F, 1000F) * clamp(calcHeight(), 0F, 1000F)

/**
 * Create a rectangle of the overlap of this rectangle and another. Note that if the two rectangles
 * do not overlap, this can create a negative area rectangle.
 */
internal fun RectForm.overlapWith(other: RectForm) =
    rectForm(
        kotlin.math.max(this.left(), other.left()),
        kotlin.math.max(this.top(), other.top()),
        kotlin.math.min(this.right(), other.right()),
        kotlin.math.min(this.bottom(), other.bottom())
    )

package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Whether the Embedded Payment Element is actually on screen, so an inline Link verification code isn't sent to
 * customers who never see it.
 */
internal object EmbeddedVisibility {
    /**
     * The most of the element that needs to be on screen, in dp, for elements taller than twice this.
     */
    const val MAX_REQUIRED_VISIBLE_HEIGHT_DP = 100f
    private const val REQUIRED_VISIBLE_FRACTION = 0.5f
    private const val MIN_VISIBLE_ALPHA = 0.01f

    data class Snapshot(
        val isAttachedToWindow: Boolean,
        val hasWindowFocus: Boolean,
        val isLifecycleResumed: Boolean,
        /**
         * Whether the hosting view and all of its ancestors are visible.
         */
        val isViewShown: Boolean,
        /**
         * The lowest alpha of the hosting view and its ancestors.
         */
        val minAncestorAlpha: Float,
        /**
         * The element's height, in px.
         */
        val height: Float,
        /**
         * The element's bounds in the window, clipped by Compose ancestors.
         */
        val boundsInWindow: Rect,
        /**
         * The part of the hosting view that isn't clipped by its View ancestors, in window coordinates. Null when
         * none of it is on screen.
         */
        val viewVisibleRectInWindow: Rect?,
        /**
         * Whether a sheet, dialog or confirmation is shown over the element.
         */
        val isUiPresentedOverHost: Boolean,
        val density: Float,
    )

    fun isVisible(snapshot: Snapshot): Boolean {
        if (!snapshot.isAttachedToWindow || !snapshot.hasWindowFocus || !snapshot.isLifecycleResumed) return false
        if (!snapshot.isViewShown || snapshot.minAncestorAlpha < MIN_VISIBLE_ALPHA) return false
        if (snapshot.isUiPresentedOverHost || snapshot.height <= 0f) return false
        val viewVisibleRect = snapshot.viewVisibleRectInWindow ?: return false
        val visibleTop = max(snapshot.boundsInWindow.top, viewVisibleRect.top)
        val visibleBottom = min(snapshot.boundsInWindow.bottom, viewVisibleRect.bottom)
        val visibleHeight = visibleBottom - visibleTop
        val requiredHeight = min(
            snapshot.height * REQUIRED_VISIBLE_FRACTION,
            MAX_REQUIRED_VISIBLE_HEIGHT_DP * snapshot.density,
        )
        return visibleHeight >= requiredHeight
    }
}

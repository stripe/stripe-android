package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.DialogReason
import kotlin.math.max
import kotlin.math.min

/**
 * Where the inline Link card's code can be typed with the keyboard up. Everything is in window pixels, measured
 * from the top, so it's independent of the views measuring it.
 */
internal object EmbeddedKeyboardGeometry {
    /**
     * A vertical span of the window.
     */
    data class Span(val top: Float, val bottom: Float) {
        val height: Float
            get() = bottom - top
    }

    sealed interface Decision {
        data object Inline : Decision

        data class Dialog(val reason: DialogReason) : Decision
    }

    /**
     * Predicts the keyboard's height before it opens.
     *
     * @param lastObservedHeight the height last observed in this process, if any.
     * @param bottomInset the bottom system inset, e.g. the navigation bar, which the keyboard's inset includes.
     */
    fun predictKeyboardHeight(
        lastObservedHeight: Float?,
        isLandscape: Boolean,
        isTablet: Boolean,
        bottomInset: Float,
        density: Float,
    ): Float {
        if (lastObservedHeight != null && lastObservedHeight > 0f) return lastObservedHeight
        val keysHeight = when {
            isTablet -> TABLET_KEYS_HEIGHT_DP
            isLandscape -> LANDSCAPE_KEYS_HEIGHT_DP
            else -> PORTRAIT_KEYS_HEIGHT_DP
        }
        return (keysHeight + SUGGESTION_BAR_HEIGHT_DP) * density + bottomInset
    }

    /**
     * Decides where the code is typed.
     *
     * @param region the card's focus region, or null when it isn't on screen.
     * @param visibleArea the part of the window that shows the element's host.
     */
    fun decide(
        region: Span?,
        visibleArea: Span?,
        hasScrollContainer: Boolean,
        keyboardTop: Float,
        margin: Float,
    ): Decision {
        if (region == null || visibleArea == null) return Decision.Dialog(DialogReason.NotInWindow)
        if (!hasScrollContainer) {
            // Nothing can move the region, so it has to be above the keyboard already.
            return if (region.bottom + margin <= keyboardTop) {
                Decision.Inline
            } else {
                Decision.Dialog(DialogReason.NoScrollView)
            }
        }
        val available = Span(top = visibleArea.top, bottom = min(visibleArea.bottom, keyboardTop))
        return if (canFit(region.height, available, margin)) {
            Decision.Inline
        } else {
            Decision.Dialog(DialogReason.DoesNotFit)
        }
    }

    /**
     * Whether a region of [height], with [margin] on each side, fits in [available].
     */
    fun canFit(height: Float, available: Span, margin: Float): Boolean {
        return height + 2 * margin <= available.height
    }

    /**
     * The span to bring into view, relative to the region's top. It's extended below by how much the keyboard
     * overlaps the host, so a scroll container that doesn't shrink for the keyboard still scrolls the region above it.
     *
     * @param hostBottom the bottom of the view hosting the element.
     */
    fun bringIntoViewSpan(
        regionHeight: Float,
        hostBottom: Float,
        keyboardTop: Float,
        margin: Float,
    ): Span {
        val keyboardOverlap = max(0f, hostBottom - keyboardTop)
        return Span(top = -margin, bottom = regionHeight + margin + keyboardOverlap)
    }

    /**
     * How much of [region], with [margin] below it, is behind the keyboard or below the visible area.
     */
    fun obscuredBelow(region: Span, visibleArea: Span, keyboardTop: Float, margin: Float): Float {
        return max(0f, region.bottom + margin - min(visibleArea.bottom, keyboardTop))
    }

    /**
     * How much of [region] is hidden by the keyboard or clipped outside [visibleArea].
     */
    fun obscuredAmount(region: Span, visibleArea: Span?, keyboardTop: Float): Float {
        visibleArea ?: return region.height
        val hiddenBelow = (region.bottom - min(visibleArea.bottom, keyboardTop)).coerceIn(0f, region.height)
        val hiddenAbove = (visibleArea.top - region.top).coerceIn(0f, region.height)
        return min(region.height, hiddenBelow + hiddenAbove)
    }

    const val MARGIN_DP = 16f

    /**
     * Below this, a hidden part of the region is rounding rather than the keyboard covering it.
     */
    const val OBSCURED_TOLERANCE_DP = 0.5f

    private const val PORTRAIT_KEYS_HEIGHT_DP = 216f
    private const val LANDSCAPE_KEYS_HEIGHT_DP = 162f
    private const val TABLET_KEYS_HEIGHT_DP = 320f
    private const val SUGGESTION_BAR_HEIGHT_DP = 44f
}

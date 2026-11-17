package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.MARGIN_DP
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.OBSCURED_TOLERANCE_DP
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.Span
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the inline Link card's focus region above the keyboard while the code is typed in the card.
 *
 * On every change it scrolls the region, with margins, above the keyboard, adding bottom [padding] only when the
 * content can't scroll far enough. It adjusts again once the keyboard settles, since some hosts move their content
 * themselves. If the region is still hidden after that, it stops and calls [onObscured].
 */
internal class EmbeddedKeyboardAvoider(
    private val measure: () -> Measurement?,
    /**
     * Brings a span, relative to the region's top, into view in every scroll container around it.
     */
    private val bringIntoView: suspend (Span) -> Unit,
    /**
     * Waits until added padding is laid out.
     */
    private val awaitLayout: suspend () -> Unit,
    private val keyboardHeights: EmbeddedKeyboardHeights,
    private val onObscured: () -> Unit,
    private val scope: CoroutineScope,
) {
    data class Measurement(
        /**
         * The focus region in the window, including any part that's clipped.
         */
        val region: Span,
        /**
         * The part of the region its ancestors don't clip, or null when all of it is clipped.
         */
        val regionVisibleArea: Span?,
        /**
         * The part of the window that shows the element's host view.
         */
        val hostVisibleArea: Span,
        val keyboardTop: Float,
        val keyboardHeight: Float,
        val hasScrollContainer: Boolean,
        val isLandscape: Boolean,
        val density: Float,
    )

    private val _padding = MutableStateFlow(0f)

    /**
     * Temporary padding below the element, in pixels.
     */
    val padding: StateFlow<Float> = _padding.asStateFlow()

    private var isActive = false
    private var adjustment: Job? = null

    fun start() {
        if (isActive) return
        isActive = true
        onLayoutChanged()
    }

    /**
     * Call when the keyboard or the region's size changes.
     */
    fun onLayoutChanged() {
        if (!isActive) return
        adjustment?.cancel()
        adjustment = scope.launch {
            if (!adjust()) return@launch
            delay(SETTLE_DELAY_MILLIS)
            if (!adjust()) return@launch
            val measurement = measure() ?: return@launch
            val obscured = EmbeddedKeyboardGeometry.obscuredAmount(
                region = measurement.region,
                visibleArea = measurement.regionVisibleArea,
                keyboardTop = measurement.keyboardTop,
            )
            if (obscured > OBSCURED_TOLERANCE_DP * measurement.density) {
                stop()
                onObscured()
            }
        }
    }

    fun stop() {
        isActive = false
        adjustment?.cancel()
        adjustment = null
        _padding.value = 0f
    }

    /**
     * @return whether there's a keyboard to keep the region above.
     */
    private suspend fun adjust(): Boolean {
        val measurement = measure() ?: return false
        // An empty keyboard frame can't be placed. It moves nothing and isn't remembered.
        if (measurement.keyboardHeight <= 0f) return false
        keyboardHeights.record(
            heightDp = measurement.keyboardHeight / measurement.density,
            isLandscape = measurement.isLandscape,
        )
        bringIntoView(measurement)
        val scrolled = measure() ?: return true
        val margin = MARGIN_DP * scrolled.density
        val obscured = EmbeddedKeyboardGeometry.obscuredBelow(
            region = scrolled.region,
            visibleArea = scrolled.hostVisibleArea,
            keyboardTop = scrolled.keyboardTop,
            margin = margin,
        )
        if (scrolled.hasScrollContainer && obscured > OBSCURED_TOLERANCE_DP * scrolled.density) {
            // The content can't scroll far enough, so it gets room below.
            _padding.value += obscured
            awaitLayout()
            measure()?.let { bringIntoView(it) }
        }
        return true
    }

    private suspend fun bringIntoView(measurement: Measurement) {
        bringIntoView(
            EmbeddedKeyboardGeometry.bringIntoViewSpan(
                regionHeight = measurement.region.height,
                hostBottom = measurement.hostVisibleArea.bottom,
                keyboardTop = measurement.keyboardTop,
                margin = MARGIN_DP * measurement.density,
            )
        )
    }

    companion object {
        /**
         * How long after the last change the keyboard counts as settled.
         */
        const val SETTLE_DELAY_MILLIS = 300L
    }
}

/**
 * The keyboard heights last observed in this process, which predict the next keyboard's height.
 */
internal class EmbeddedKeyboardHeights {
    private var portraitHeightDp: Float? = null
    private var landscapeHeightDp: Float? = null

    @Synchronized
    fun record(heightDp: Float, isLandscape: Boolean) {
        if (heightDp <= 0f) return
        if (isLandscape) landscapeHeightDp = heightDp else portraitHeightDp = heightDp
    }

    @Synchronized
    fun lastObservedDp(isLandscape: Boolean): Float? = if (isLandscape) landscapeHeightDp else portraitHeightDp

    companion object {
        val shared = EmbeddedKeyboardHeights()
    }
}

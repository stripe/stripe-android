package com.stripe.android.paymentelement.embedded.linkinline

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.MARGIN_DP
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.Span
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.CodeEntry
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import android.graphics.Rect as AndroidRect

/**
 * Lets the inline Link card's code be typed in place. It decides whether the layout can keep the card's focus
 * region above the keyboard, and keeps it there while the code is typed.
 */
@Stable
internal class EmbeddedLinkInlineTyping(
    private val keyboardHeights: EmbeddedKeyboardHeights = EmbeddedKeyboardHeights.shared,
) {
    private var regionCoordinates: LayoutCoordinates? = null
    private var hasScrollContainer = false
    private val bringIntoViewRequester = BringIntoViewRequester()

    /**
     * Temporary room below the element, in pixels, for content that can't scroll the card far enough.
     */
    var paddingPx by mutableFloatStateOf(0f)
        private set

    /**
     * Apply to the card's focus region: the title, subtitle, code, error and action.
     */
    val focusRegionModifier: Modifier = Modifier
        .onGloballyPositioned { regionCoordinates = it }
        .bringIntoViewRequester(bringIntoViewRequester)

    /**
     * Apply to the element's outer container. A scroll container measures its content with an unbounded height.
     */
    val containerModifier: Modifier = Modifier.layout { measurable, constraints ->
        hasScrollContainer = !constraints.hasBoundedHeight
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    fun codeEntry(view: View): CodeEntry {
        val measurement = measure(view)
        val decision = EmbeddedKeyboardGeometry.decide(
            region = measurement?.region,
            visibleArea = measurement?.hostVisibleArea,
            hasScrollContainer = hasScrollContainer,
            keyboardTop = measurement?.let { predictedKeyboardTop(view, it) } ?: 0f,
            margin = MARGIN_DP * view.resources.displayMetrics.density,
        )
        return when (decision) {
            is EmbeddedKeyboardGeometry.Decision.Inline -> CodeEntry.Inline
            is EmbeddedKeyboardGeometry.Decision.Dialog -> CodeEntry.Dialog(decision.reason)
        }
    }

    fun measure(view: View): EmbeddedKeyboardAvoider.Measurement? {
        val coordinates = regionCoordinates?.takeIf { it.isAttached } ?: return null
        val hostVisibleRect = AndroidRect()
        if (!view.isAttachedToWindow || !view.getGlobalVisibleRect(hostVisibleRect)) return null
        // The global visible rect is relative to the root view, which can be offset in its window.
        val rootLocation = IntArray(2)
        view.rootView.getLocationInWindow(rootLocation)
        val hostVisibleArea = Span(
            top = (hostVisibleRect.top + rootLocation[1]).toFloat(),
            bottom = (hostVisibleRect.bottom + rootLocation[1]).toFloat(),
        )
        val regionTop = coordinates.positionInWindow().y
        val clippedRegion = coordinates.boundsInWindow()
        val regionVisibleArea = Span(
            top = max(clippedRegion.top, hostVisibleArea.top),
            bottom = min(clippedRegion.bottom, hostVisibleArea.bottom),
        )
        val keyboardHeight = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.ime())
            ?.bottom
            ?.toFloat()
            ?: 0f
        return EmbeddedKeyboardAvoider.Measurement(
            region = Span(top = regionTop, bottom = regionTop + coordinates.size.height),
            regionVisibleArea = regionVisibleArea.takeIf { it.height > 0f },
            hostVisibleArea = hostVisibleArea,
            keyboardTop = rootLocation[1] + view.rootView.height - keyboardHeight,
            keyboardHeight = keyboardHeight,
            hasScrollContainer = hasScrollContainer,
            isLandscape = view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            density = view.resources.displayMetrics.density,
        )
    }

    suspend fun bringIntoView(span: Span) {
        val width = regionCoordinates?.size?.width ?: return
        bringIntoViewRequester.bringIntoView(Rect(0f, span.top, width.toFloat(), span.bottom))
    }

    fun setPadding(paddingPx: Float) {
        this.paddingPx = paddingPx
    }

    private fun predictedKeyboardTop(view: View, measurement: EmbeddedKeyboardAvoider.Measurement): Float {
        // A keyboard that's already up, e.g. from another field, is measured rather than predicted.
        if (measurement.keyboardHeight > 0f) return measurement.keyboardTop
        val bottomInset = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?.toFloat()
            ?: 0f
        val keyboardHeight = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = keyboardHeights.lastObservedDp(measurement.isLandscape)?.times(measurement.density),
            isLandscape = measurement.isLandscape,
            isTablet = view.resources.configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP,
            bottomInset = bottomInset,
            density = measurement.density,
        )
        return measurement.keyboardTop - keyboardHeight
    }

    private companion object {
        const val TABLET_SMALLEST_WIDTH_DP = 600
    }
}

/**
 * Remembers the [EmbeddedLinkInlineTyping] for the element, and keeps the card above the keyboard while its code is
 * typed. Checks every [KEYBOARD_POLL_INTERVAL_MILLIS] for keyboard and card changes while typing.
 */
@Composable
internal fun rememberEmbeddedLinkInlineTyping(linkInlineAuth: EmbeddedLinkInlineAuth): EmbeddedLinkInlineTyping {
    val view = LocalView.current
    val typing = remember { EmbeddedLinkInlineTyping() }
    DisposableEffect(linkInlineAuth, view, typing) {
        val registration = linkInlineAuth.registerTypingHost { typing.codeEntry(view) }
        onDispose { registration.close() }
    }

    val isTyping by linkInlineAuth.isTyping.collectAsState()
    if (isTyping) {
        val currentLinkInlineAuth by rememberUpdatedState(linkInlineAuth)
        LaunchedEffect(view, typing) {
            val avoider = EmbeddedKeyboardAvoider(
                measure = { typing.measure(view) },
                bringIntoView = typing::bringIntoView,
                awaitLayout = { repeat(LAYOUT_FRAMES) { withFrameNanos {} } },
                keyboardHeights = EmbeddedKeyboardHeights.shared,
                onObscured = { currentLinkInlineAuth.onTypingObscured() },
                scope = this,
            )
            launch {
                avoider.padding.collect(typing::setPadding)
            }
            try {
                avoider.start()
                var lastLayout: Pair<Float, Float>? = null
                while (true) {
                    val layout = typing.measure(view)?.let { it.keyboardHeight to it.region.height }
                    if (layout != lastLayout) {
                        lastLayout = layout
                        avoider.onLayoutChanged()
                    }
                    delay(KEYBOARD_POLL_INTERVAL_MILLIS)
                }
            } finally {
                avoider.stop()
                typing.setPadding(0f)
            }
        }
    }
    return typing
}

/**
 * The temporary room below the element while the card's code is typed.
 */
@Composable
internal fun EmbeddedLinkInlineTypingPadding(typing: EmbeddedLinkInlineTyping) {
    val height = with(LocalDensity.current) { typing.paddingPx.toDp() }
    Spacer(modifier = Modifier.height(height))
}

internal const val KEYBOARD_POLL_INTERVAL_MILLIS = 50L

/**
 * Frames to wait for added padding to be measured and placed.
 */
private const val LAYOUT_FRAMES = 2

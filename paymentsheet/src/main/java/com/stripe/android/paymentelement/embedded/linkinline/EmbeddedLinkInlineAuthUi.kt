package com.stripe.android.paymentelement.embedded.linkinline

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.link.ui.verification.VerificationDialogBody
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import android.graphics.Rect as AndroidRect

/**
 * Remembers where the Embedded Payment Element is laid out, and reports to [EmbeddedLinkInlineAuth] the first time
 * it's on screen. Checks every [VISIBILITY_POLL_INTERVAL_MILLIS], including while scrolling, until then.
 *
 * Apply the returned modifier to the element's outer container, which stays in place across `configure` calls.
 */
@Composable
internal fun rememberEmbeddedVisibilityModifier(linkInlineAuth: EmbeddedLinkInlineAuth): Modifier {
    val needsVisibility by linkInlineAuth.needsVisibility.collectAsState()
    val layout = remember { LayoutHolder() }
    if (needsVisibility) {
        val view = LocalView.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val currentLinkInlineAuth by rememberUpdatedState(linkInlineAuth)
        LaunchedEffect(view, lifecycle) {
            while (true) {
                val snapshot = layout.coordinates?.let { coordinates ->
                    visibilitySnapshot(
                        view = view,
                        lifecycle = lifecycle,
                        coordinates = coordinates,
                        isUiPresentedOverHost = currentLinkInlineAuth.isUiPresentedOverElement(),
                    )
                }
                if (snapshot != null && EmbeddedVisibility.isVisible(snapshot)) {
                    currentLinkInlineAuth.onElementVisible()
                    break
                }
                delay(VISIBILITY_POLL_INTERVAL_MILLIS)
            }
        }
    }
    return remember(layout) {
        Modifier.onGloballyPositioned { layout.coordinates = it }
    }
}

/**
 * Shows the Link dialog for an inline verification attempt over the screen hosting the Embedded Payment Element.
 */
@Composable
internal fun EmbeddedLinkVerificationDialogHost(linkInlineAuth: EmbeddedLinkInlineAuth) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(linkInlineAuth, view, lifecycle) {
        val registration = linkInlineAuth.registerDialogHost(
            isReady = { view.hasWindowFocus() && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
        )
        onDispose { registration.close() }
    }

    val dialog by linkInlineAuth.dialog.collectAsState()
    dialog?.let { session ->
        val state by session.viewState.collectAsState()
        VerificationDialogBody(
            state = state,
            otpElement = session.otpElement,
            phoneNumberController = session.phoneNumberController,
            onBack = session::onBack,
            onNavigateBack = session::onNavigateBack,
            onFocusRequested = session::onFocusRequested,
            didShowCodeSentNotification = {},
            onChangeEmailClick = session::onChangeEmail,
            onResendCodeClick = session::onResendCode,
            onEmailCodeClick = session::onEmailCode,
            onPhoneNumberSubmitted = session::onPhoneNumberSubmitted,
            onConsentShown = {},
        )
    }
}

private class LayoutHolder {
    var coordinates: LayoutCoordinates? = null
}

private fun visibilitySnapshot(
    view: View,
    lifecycle: Lifecycle,
    coordinates: LayoutCoordinates,
    isUiPresentedOverHost: Boolean,
): EmbeddedVisibility.Snapshot {
    val isAttached = coordinates.isAttached && view.isAttachedToWindow
    val viewVisibleRect = AndroidRect()
    val isAnyOfViewVisible = view.getGlobalVisibleRect(viewVisibleRect)
    // The global visible rect is relative to the root view, which can be offset in its window.
    val rootLocation = IntArray(2)
    view.rootView.getLocationInWindow(rootLocation)
    return EmbeddedVisibility.Snapshot(
        isAttachedToWindow = isAttached,
        hasWindowFocus = view.hasWindowFocus(),
        isLifecycleResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        isViewShown = view.isShown,
        minAncestorAlpha = view.minAncestorAlpha(),
        height = coordinates.size.height.toFloat(),
        boundsInWindow = if (isAttached) coordinates.boundsInWindow() else Rect.Zero,
        viewVisibleRectInWindow = if (isAnyOfViewVisible) {
            Rect(
                left = (viewVisibleRect.left + rootLocation[0]).toFloat(),
                top = (viewVisibleRect.top + rootLocation[1]).toFloat(),
                right = (viewVisibleRect.right + rootLocation[0]).toFloat(),
                bottom = (viewVisibleRect.bottom + rootLocation[1]).toFloat(),
            )
        } else {
            null
        },
        isUiPresentedOverHost = isUiPresentedOverHost,
        density = view.resources.displayMetrics.density,
    )
}

private fun View.minAncestorAlpha(): Float {
    var minAlpha = alpha
    var parent = parent
    while (parent is View) {
        minAlpha = minOf(minAlpha, parent.alpha)
        parent = parent.parent
    }
    return minAlpha
}

internal const val VISIBILITY_POLL_INTERVAL_MILLIS = 250L

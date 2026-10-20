package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState

/**
 * Shows [content] in Link's own sheet, with Link's container, app bar and close button, so the screens
 * another Stripe SDK shows around Link's authentication look like part of it. The content keeps the
 * caller's theme and gets Link's horizontal padding.
 *
 * @param visible Whether the sheet is shown. It animates in and out as this changes.
 * @param onClose Called when the user taps the close button or the area above the sheet. The sheet stays until
 * the caller hides it by setting [visible] to false.
 */
@OptIn(LinkControllerPreview::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun LinkHostedSheet(
    visible: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    var isComposed by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) isComposed = true
    }
    if (!isComposed) return

    // Remembered only while composed: each showing starts from a fresh, hidden sheet.
    val state = rememberCallerHiddenSheetState(visible, onClose)
    var isHiding by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) {
            isHiding = true
            state.hide()
            isComposed = false
        } else if (isHiding) {
            // Showing again cancels the hide part-way down, and StripeBottomSheetLayout only shows the sheet once.
            isHiding = false
            state.modalBottomSheetState.show()
        }
    }

    val callerColors = MaterialTheme.colors
    val callerTypography = MaterialTheme.typography
    val callerShapes = MaterialTheme.shapes
    // Link's surface follows the caller's light or dark theme, so the caller's content stays readable on it.
    val appearance = remember(callerColors.isLight) {
        val style = if (callerColors.isLight) LinkAppearance.Style.ALWAYS_LIGHT else LinkAppearance.Style.ALWAYS_DARK
        LinkAppearance().style(style).build()
    }

    // Unlike ElementsBottomSheetLayout, leaves the caller's system bars alone.
    StripeBottomSheetLayout(
        state = state,
        layoutInfo = rememberStripeBottomSheetLayoutInfo(
            cornerRadius = 24.dp,
            scrimColor = Color.Black.copy(alpha = 0.32f),
        ),
        onDismissed = onClose,
    ) {
        DefaultLinkTheme(appearance = appearance) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LinkTheme.colors.surfacePrimary)
            ) {
                LinkAppBar(
                    state = LinkAppBarState.initial(LinkBrand.Link),
                    onBackPressed = onClose,
                )
                MaterialTheme(
                    colors = callerColors,
                    typography = callerTypography,
                    shapes = callerShapes,
                ) {
                    // Link's own screens pad for the navigation bar themselves. The keyboard's inset is consumed
                    // above, so this padding gives way while the keyboard is open.
                    Box(
                        modifier = Modifier
                            .padding(horizontal = HorizontalPadding)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/** A sheet state that never hides itself while [visible]: a tap above the sheet calls [onClose] instead. */
@Composable
private fun rememberCallerHiddenSheetState(visible: Boolean, onClose: () -> Unit): StripeBottomSheetState {
    val currentVisible by rememberUpdatedState(visible)
    val currentOnClose by rememberUpdatedState(onClose)
    return rememberStripeBottomSheetState(
        confirmValueChange = { value ->
            if (value == ModalBottomSheetValue.Hidden && currentVisible) {
                currentOnClose()
                false
            } else {
                true
            }
        },
    )
}

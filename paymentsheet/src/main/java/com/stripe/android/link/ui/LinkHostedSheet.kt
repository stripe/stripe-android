package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.SheetCornerRadius
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState

/**
 * Shows [content] in Link's own sheet, with Link's container, app bar and close button, so the screens
 * another Stripe SDK shows around Link's authentication look like part of it. The content keeps the
 * caller's theme and gets Link's horizontal padding.
 *
 * @param visible Whether the sheet is shown. It animates in and out as this changes.
 * @param onClose Called when the user taps the close button or the area above the sheet. The caller hides
 * the sheet by setting [visible] to false.
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
    val state = rememberStripeBottomSheetState()
    LaunchedEffect(visible) {
        if (!visible) {
            state.hide()
            isComposed = false
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
            cornerRadius = SheetCornerRadius,
            scrimColor = Color.Black.copy(alpha = 0.32f),
        ),
        // The caller's screen already sits below the status bar and above the keyboard.
        modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars.union(WindowInsets.ime)),
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
                    Box(modifier = Modifier.padding(horizontal = HorizontalPadding)) {
                        content()
                    }
                }
            }
        }
    }
}

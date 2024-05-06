package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState.DismissalType
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val BottomSheetContentTestTag = "BottomSheetContentTestTag"

/**
 * Renders the provided [sheetContent] in a modal bottom sheet.
 *
 * @param state The [StripeBottomSheetState] that controls the visibility of the bottom sheet.
 * navigate to a specific screen.
 * @param onUpdateStatusBarColor Called when the status bar color needs to be updated. This is based
 * on the expansion state of the sheet.
 * @param onDismissed Called when the user dismisses the bottom sheet by swiping down. You should
 * inform your view model about this change.
 * @param sheetContent The content to render in the sheet
 */
@OptIn(ExperimentalMaterialApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun StripeBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onUpdateStatusBarColor: (Color) -> Unit = {},
    onDismissed: () -> Unit,
    sheetContent: @Composable () -> Unit,
) {
    val scrimColor = ModalBottomSheetDefaults.scrimColor
    val isExpanded = state.modalBottomSheetState.targetValue == Expanded

    val statusBarColorAlpha by animateFloatAsState(
        targetValue = if (isExpanded) scrimColor.alpha else 0f,
        animationSpec = tween(),
        label = "StatusBarColorAlpha",
    )

    LaunchedEffect(statusBarColorAlpha) {
        onUpdateStatusBarColor(scrimColor.copy(statusBarColorAlpha))
    }

    LaunchedEffect(Unit) {
        state.show()

        val dismissalType = state.awaitDismissal()
        if (dismissalType == DismissalType.SwipedDownByUser) {
            onDismissed()
        }
    }

    ModalBottomSheetLayout(
        modifier = modifier
            .statusBarsPadding()
            .imePadding(),
        sheetState = state.modalBottomSheetState,
        sheetShape = RoundedCornerShape(
            topStart = MaterialTheme.stripeShapes.cornerRadius.dp,
            topEnd = MaterialTheme.stripeShapes.cornerRadius.dp,
        ),
        sheetGesturesEnabled = false,
        sheetElevation = 0.dp,
        sheetContent = {
            Box(modifier = Modifier.testTag(BottomSheetContentTestTag)) {
                sheetContent()
            }
        },
        content = {},
    )
}

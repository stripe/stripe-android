package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState.DismissalType

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val BottomSheetContentTestTag = "BottomSheetContentTestTag"

/**
 * Renders the provided [sheetContent] in a modal bottom sheet.
 *
 * @param state The [StripeBottomSheetState] that controls the visibility of the bottom sheet.
 * navigate to a specific screen.
 * @param layoutInfo The [StripeBottomSheetLayoutInfo] that controls how the bottom sheet is styled
 * @param onDismissed Called when the user dismisses the bottom sheet by swiping down. You should
 * inform your view model about this change.
 * @param sheetContent The content to render in the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun StripeBottomSheetLayout(
    state: StripeBottomSheetState,
    layoutInfo: StripeBottomSheetLayoutInfo,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    sheetContent: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (!state.modalBottomSheetState.isVisible) {
            state.show()
        }

        val dismissalType = state.awaitDismissal()
        if (dismissalType == DismissalType.SwipedDownByUser) {
            onDismissed()
        }
    }

    ModalBottomSheet(
        modifier = modifier
            .statusBarsPadding()
            .imePadding(),
        scrimColor = layoutInfo.scrimColor,
        containerColor = layoutInfo.sheetBackgroundColor,
        tonalElevation = 0.dp,
        sheetGesturesEnabled = false,
        shape = layoutInfo.sheetShape,
        sheetState = state.modalBottomSheetState,
        dragHandle = null,
        onDismissRequest = {}
    ) {
        Box(modifier = Modifier.testTag(BottomSheetContentTestTag)) {
            sheetContent()
        }
    }
}

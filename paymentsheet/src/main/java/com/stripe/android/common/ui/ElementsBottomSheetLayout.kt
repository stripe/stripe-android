package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberBottomSheetKeyboardHandler
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo

@Composable
internal fun ElementsBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    val keyboardHandler = rememberBottomSheetKeyboardHandler()
    val layoutInfo = rememberStripeBottomSheetLayoutInfo()

    LaunchedEffect(Unit) {
        state.addInterceptor(keyboardHandler)
    }

    LaunchedEffect(systemUiController) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
        )
    }

    StripeBottomSheetLayout(
        state = state,
        layoutInfo = layoutInfo,
        onUpdateStatusBarColor = { color ->
            systemUiController.setStatusBarColor(
                color = color,
                darkIcons = false,
            )
        },
        onDismissed = onDismissed,
        sheetContent = content,
    )
}

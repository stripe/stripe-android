package com.stripe.android.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterialApi::class)
internal class BottomSheetState(
    val modalBottomSheetState: ModalBottomSheetState,
) {

    suspend fun show() {
        modalBottomSheetState.show()
    }

    suspend fun awaitDismissal() {
        snapshotFlow { modalBottomSheetState.isVisible }.first { isVisible -> !isVisible }
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun rememberBottomSheetState(
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
): BottomSheetState {
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = confirmValueChange,
        skipHalfExpanded = true,
        animationSpec = tween(),
    )

    return remember {
        BottomSheetState(modalBottomSheetState)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    onShow: () -> Unit = {},
    onDismissed: () -> Unit = {},
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    val scrimColor = ModalBottomSheetDefaults.scrimColor

    val isExpanded = state.modalBottomSheetState.targetValue == Expanded

    val statusBarColorAlpha by animateFloatAsState(
        targetValue = if (isExpanded) scrimColor.alpha else 0f,
        animationSpec = tween(),
        label = "StatusBarColorAlpha",
    )

    LaunchedEffect(Unit) {
        state.show()
        onShow()

        state.awaitDismissal()
        onDismissed()
    }

    DisposableEffect(systemUiController, statusBarColorAlpha) {
        systemUiController.setStatusBarColor(
            color = scrimColor.copy(statusBarColorAlpha),
            darkIcons = false,
        )

        onDispose {}
    }

    DisposableEffect(systemUiController) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
        )

        onDispose {}
    }

    ModalBottomSheetLayout(
        modifier = modifier.statusBarsPadding(),
        sheetState = state.modalBottomSheetState,
        sheetContent = {
            sheetContent()
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        },
        content = {},
    )
}

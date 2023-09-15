package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalComposeUiApi::class)
internal class BottomSheetKeyboardHandler(
    private val keyboardController: SoftwareKeyboardController?,
    private val isKeyboardVisible: State<Boolean>,
) {

    suspend fun dismiss() {
        if (isKeyboardVisible.value) {
            keyboardController?.hide()
            awaitKeyboardDismissed()
        }
    }

    private suspend fun awaitKeyboardDismissed() {
        snapshotFlow { isKeyboardVisible.value }.first { !it }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun rememberBottomSheetKeyboardHandler(): BottomSheetKeyboardHandler {
    val view = LocalView.current
    val isImeVisible = remember { mutableStateOf(false) }

    DisposableEffect(view) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            isImeVisible.value = insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }

        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    return BottomSheetKeyboardHandler(keyboardController, isImeVisible)
}

package com.stripe.android.common.ui

import android.util.Log
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
            Log.d("TILL123", "Hiding keyboard…")
            keyboardController?.hide()
            awaitKeyboardDismissed()
            Log.d("TILL123", "Keyboard hidden!")
        } else {
            Log.d("TILL123", "Keyboard already hidden!")
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
            val isVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            isImeVisible.value = isVisible
            Log.d("TILL123", "Is IME visible: $isVisible")
            insets
        }

        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    return BottomSheetKeyboardHandler(keyboardController, isImeVisible)
}

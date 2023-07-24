package com.stripe.android.common.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalComposeUiApi::class)
internal class BottomSheetKeyboardHandler(
    private val keyboardController: SoftwareKeyboardController?,
    private val isKeyboardVisible: State<Boolean>,
) {

    suspend fun dismissAndWait() {
        keyboardController?.hide()

        if (isKeyboardVisible.value) {
            awaitKeyboardDismissed()
        }
    }

    private suspend fun awaitKeyboardDismissed() {
        snapshotFlow { isKeyboardVisible.value }.first { !it }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun rememberBottomSheetKeyboardHandler(): BottomSheetKeyboardHandler {
    val isImeVisible = WindowInsets.isImeVisible
    val isImeVisibleState = rememberUpdatedState(isImeVisible)
    val keyboardController = LocalSoftwareKeyboardController.current
    return BottomSheetKeyboardHandler(keyboardController, isImeVisibleState)
}

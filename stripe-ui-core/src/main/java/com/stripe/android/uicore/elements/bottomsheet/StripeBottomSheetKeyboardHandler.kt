package com.stripe.android.uicore.elements.bottomsheet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.input.TextInputService
import kotlinx.coroutines.flow.first

internal class StripeBottomSheetKeyboardHandler(
    private val textInputService: TextInputService?,
    private val isKeyboardVisible: State<Boolean>,
) {

    suspend fun dismiss() {
        if (isKeyboardVisible.value) {
            @Suppress("DEPRECATION")
            textInputService?.hideSoftwareKeyboard()
            awaitKeyboardDismissed()
        }
    }

    private suspend fun awaitKeyboardDismissed() {
        snapshotFlow { isKeyboardVisible.value }.first { !it }
    }
}

@Composable
internal fun rememberStripeBottomSheetKeyboardHandler(): StripeBottomSheetKeyboardHandler {
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    val isImeVisibleState = rememberUpdatedState(newValue = imeHeight > 0)
    val textInputService = LocalTextInputService.current
    return remember {
        StripeBottomSheetKeyboardHandler(textInputService, isImeVisibleState)
    }
}

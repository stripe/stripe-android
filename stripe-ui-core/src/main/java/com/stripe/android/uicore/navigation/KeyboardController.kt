package com.stripe.android.uicore.navigation

import android.view.ViewTreeObserver
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.first

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class KeyboardController(
    private val dismissKeyboard: () -> Unit,
    private val isKeyboardVisible: State<Boolean>,
) {

    suspend fun dismiss() {
        if (isKeyboardVisible.value) {
            dismissKeyboard()
            awaitKeyboardDismissed()
        }
    }

    private suspend fun awaitKeyboardDismissed() {
        snapshotFlow { isKeyboardVisible.value }.first { !it }
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun rememberKeyboardController(): KeyboardController {
    val textInputService = LocalTextInputService.current
    val keyboardState = isKeyboardVisibleAsState()

    return KeyboardController(
        dismissKeyboard = {
            // We're using this method because LocalSoftwareKeyboardController is
            // still experimental in Compose 1.5. We should switch over once we update
            // to Compose 1.6, in which the experimental state has been removed.
            textInputService?.hideSoftwareKeyboard()
        },
        isKeyboardVisible = keyboardState,
    )
}

@Composable
private fun isKeyboardVisibleAsState(): State<Boolean> {
    val view = LocalView.current
    val state = remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            val isKeyboardOpen = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
            state.value = isKeyboardOpen
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return state
}

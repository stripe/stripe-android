package com.stripe.android.uicore.elements.bottomsheet

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.input.TextInputService
import com.stripe.android.uicore.BuildConfig
import kotlinx.coroutines.flow.first

@RestrictTo(RestrictTo.Scope.LIBRARY)
class BottomSheetKeyboardHandler internal constructor(
    private val textInputService: TextInputService?,
    private val isKeyboardVisible: State<Boolean>,
) : StripeBottomSheetState.DismissalInterceptor {

    override suspend fun onBottomSheetDismissal() {
        if (skipHideAnimation) {
            // TODO: Explain
            return
        }

        // We dismiss the keyboard before we dismiss the sheet. This looks cleaner and prevents
        // a CancellationException.
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun rememberBottomSheetKeyboardHandler(): BottomSheetKeyboardHandler {
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    val isImeVisibleState = rememberUpdatedState(newValue = imeHeight > 0)
    val textInputService = LocalTextInputService.current
    return BottomSheetKeyboardHandler(textInputService, isImeVisibleState)
}

private val skipHideAnimation: Boolean
    get() = BuildConfig.DEBUG && (isRunningUnitTest || isRunningUiTest)

private val isRunningUnitTest: Boolean
    get() {
        return runCatching {
            Build.FINGERPRINT.lowercase() == "robolectric"
        }.getOrDefault(false)
    }

private val isRunningUiTest: Boolean
    get() {
        return runCatching {
            Class.forName("androidx.test.InstrumentationRegistry")
        }.isSuccess
    }

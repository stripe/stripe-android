package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberTextFocusRequester(
    focusAsk: MutableStateFlow<Boolean>
): FocusRequester {
    val isInspectionMode = LocalInspectionMode.current
    val focusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(isInspectionMode) {
        if (!isInspectionMode) {
            focusAsk.collect { shouldFocus ->
                if (shouldFocus) {
                    snapshotFlow { windowInfo.isWindowFocused }.first { it }
                    withFrameNanos {}
                    while (!focusRequester.requestFocus()) {
                        withFrameNanos {}
                    }
                    focusAsk.value = false
                }
            }
        }
    }

    return focusRequester
}

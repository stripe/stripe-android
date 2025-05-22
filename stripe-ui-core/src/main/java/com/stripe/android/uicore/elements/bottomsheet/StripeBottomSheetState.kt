package com.stripe.android.uicore.elements.bottomsheet

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.animation.core.tween
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.stripe.android.uicore.BuildConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.coroutines.cancellation.CancellationException

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun rememberStripeBottomSheetState(
    initialValue: ModalBottomSheetValue = ModalBottomSheetValue.Hidden,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
): StripeBottomSheetState {
    val dismissalType = remember { mutableStateOf<StripeBottomSheetState.DismissalType?>(null) }

    val wrappedConfirmValueChange: (ModalBottomSheetValue) -> Boolean = { value ->
        confirmValueChange(value).also { confirmed ->
            if (confirmed) {
                dismissalType.value = StripeBottomSheetState.DismissalType.UserAction
            }
        }
    }

    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = initialValue,
        confirmValueChange = wrappedConfirmValueChange,
        skipHalfExpanded = true,
        animationSpec = tween(),
    )

    val keyboardHandler = rememberStripeBottomSheetKeyboardHandler()

    val stripeBottomSheetState = remember {
        StripeBottomSheetState(
            modalBottomSheetState = modalBottomSheetState,
            keyboardHandler = keyboardHandler,
            dismissalType = dismissalType,
        )
    }

    return stripeBottomSheetState
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class StripeBottomSheetState internal constructor(
    val modalBottomSheetState: ModalBottomSheetState,
    private val keyboardHandler: StripeBottomSheetKeyboardHandler,
    private val dismissalType: MutableState<DismissalType?>,
) {

    var skipHideAnimation: Boolean = false

    suspend fun show() {
        repeatUntilSucceededOrLimit(10) {
            // Showing the bottom sheet can be interrupted.
            // We keep trying until it's fully displayed.
            modalBottomSheetState.show()
        }

        // Ensure that isVisible is being updated correctly inside ModalBottomSheetState
        snapshotFlow { modalBottomSheetState.isVisible }.first { isVisible -> isVisible }
    }

    suspend fun awaitDismissal(): DismissalType {
        return snapshotFlow { dismissalType.value?.takeIf { !modalBottomSheetState.isVisible } }
            .filterNotNull()
            .first()
    }

    suspend fun hide() {
        if (skipHideAnimation) {
            return
        }

        dismissalType.value = DismissalType.Programmatically

        // We dismiss the keyboard before we dismiss the sheet. This looks cleaner and prevents
        // a CancellationException.
        keyboardHandler.dismiss()

        if (modalBottomSheetState.isVisible) {
            repeatUntilSucceededOrLimit(10) {
                // Hiding the bottom sheet can be interrupted.
                // We keep trying until it's fully hidden.
                modalBottomSheetState.hide()
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    enum class DismissalType {
        Programmatically,
        UserAction,
    }
}

private suspend fun repeatUntilSucceededOrLimit(
    limit: Int,
    block: suspend () -> Unit
) {
    var counter = 0
    while (counter < limit) {
        try {
            block()
            break
        } catch (ignored: CancellationException) {
            counter += 1
        }
    }
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

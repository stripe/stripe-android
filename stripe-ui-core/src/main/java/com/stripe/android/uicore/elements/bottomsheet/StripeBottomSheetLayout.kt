package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeShapes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal const val BottomSheetContentTestTag = "BottomSheetContentTestTag"

@OptIn(ExperimentalMaterialApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class StripeBottomSheetState(
    val modalBottomSheetState: ModalBottomSheetState,
    val keyboardHandler: BottomSheetKeyboardHandler,
    val sheetGesturesEnabled: Boolean,
) {

    private var dismissalType: DismissalType? = null

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
        snapshotFlow { modalBottomSheetState.isVisible }.first { isVisible -> !isVisible }
        return dismissalType ?: DismissalType.SwipedDownByUser
    }

    suspend fun hide() {
        dismissalType = DismissalType.Programmatically
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
        SwipedDownByUser,
    }
}

@OptIn(ExperimentalMaterialApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun rememberStripeBottomSheetState(
    initialValue: ModalBottomSheetValue = ModalBottomSheetValue.Hidden,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
): StripeBottomSheetState {
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = initialValue,
        confirmValueChange = confirmValueChange,
        skipHalfExpanded = true,
        animationSpec = tween(),
    )

    val keyboardHandler = rememberBottomSheetKeyboardHandler()

    return remember {
        StripeBottomSheetState(
            modalBottomSheetState = modalBottomSheetState,
            keyboardHandler = keyboardHandler,
            sheetGesturesEnabled = false,
        )
    }
}

/**
 * Renders the provided [sheetContent] in a modal bottom sheet.
 *
 * @param state The [StripeBottomSheetState] that controls the visibility of the bottom sheet.
 * navigate to a specific screen.
 * @param onDismissed Called when the user dismisses the bottom sheet by swiping down. You should
 * inform your view model about this change.
 */
@OptIn(ExperimentalMaterialApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun StripeBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    sheetContent: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        state.show()

        val dismissalType = state.awaitDismissal()
        if (dismissalType == StripeBottomSheetState.DismissalType.SwipedDownByUser) {
            onDismissed()
        }
    }

    ModalBottomSheetLayout(
        modifier = modifier
            .statusBarsPadding()
            .imePadding(),
        sheetState = state.modalBottomSheetState,
        sheetShape = RoundedCornerShape(
            topStart = MaterialTheme.stripeShapes.cornerRadius.dp,
            topEnd = MaterialTheme.stripeShapes.cornerRadius.dp,
        ),
        sheetGesturesEnabled = state.sheetGesturesEnabled,
        sheetElevation = 0.dp,
        sheetContent = {
            Box(modifier = Modifier.testTag(BottomSheetContentTestTag)) {
                sheetContent()
            }
        },
        content = {},
    )
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
class BottomSheetKeyboardHandler(
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun rememberBottomSheetKeyboardHandler(): BottomSheetKeyboardHandler {
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    val isImeVisibleState = rememberUpdatedState(newValue = imeHeight > 0)
    val textInputService = LocalTextInputService.current
    return BottomSheetKeyboardHandler(textInputService, isImeVisibleState)
}

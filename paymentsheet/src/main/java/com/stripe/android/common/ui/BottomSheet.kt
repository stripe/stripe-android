package com.stripe.android.common.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.uicore.stripeShapes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal const val BottomSheetContentTestTag = "BottomSheetContentTestTag"

@OptIn(ExperimentalMaterialApi::class)
internal class BottomSheetState(
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
        if (skipHideAnimation) {
            return
        }

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

    internal enum class DismissalType {
        Programmatically,
        SwipedDownByUser,
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

    val keyboardHandler = rememberBottomSheetKeyboardHandler()

    return remember {
        BottomSheetState(
            modalBottomSheetState = modalBottomSheetState,
            keyboardHandler = keyboardHandler,
            sheetGesturesEnabled = false,
        )
    }
}

/**
 * Renders the provided [sheetContent] in a modal bottom sheet.
 *
 * @param state The [BottomSheetState] that controls the visibility of the bottom sheet.
 * navigate to a specific screen.
 * @param onDismissed Called when the user dismisses the bottom sheet by swiping down. You should
 * inform your view model about this change.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    sheetContent: @Composable () -> Unit,
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

        val dismissalType = state.awaitDismissal()
        if (dismissalType == BottomSheetState.DismissalType.SwipedDownByUser) {
            onDismissed()
        }
    }

    LaunchedEffect(systemUiController, statusBarColorAlpha) {
        systemUiController.setStatusBarColor(
            color = scrimColor.copy(statusBarColorAlpha),
            darkIcons = false,
        )
    }

    LaunchedEffect(systemUiController) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
        )
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

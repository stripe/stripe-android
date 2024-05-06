package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.tween
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

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

    return remember {
        StripeBottomSheetState(
            modalBottomSheetState = modalBottomSheetState,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class StripeBottomSheetState internal constructor(
    val modalBottomSheetState: ModalBottomSheetState,
) {

    interface DismissalInterceptor {
        suspend fun onBottomSheetDismissal()
    }

    private var dismissalType: DismissalType? = null

    private val dismissalInterceptors = mutableListOf<DismissalInterceptor>()

    fun addInterceptor(interceptor: DismissalInterceptor) {
        dismissalInterceptors += interceptor
    }

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

        for (interceptor in dismissalInterceptors) {
            interceptor.onBottomSheetDismissal()
        }

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

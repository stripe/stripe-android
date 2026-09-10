package com.stripe.android.checkout

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal interface AwaitingReadyState {
    var isAwaitingReady: Boolean
}

internal sealed interface ReadyLaunchResult {
    data object Launched : ReadyLaunchResult
    data object KeepLoading : ReadyLaunchResult
}

internal class CheckoutLoadingToReadySheetCoordinator(
    lifecycleOwner: LifecycleOwner,
    private val sheetStateHolder: SheetStateHolder,
    private val isUpdating: StateFlow<Boolean>,
    private val awaitingReadyState: AwaitingReadyState,
    private val launchPendingReady: () -> ReadyLaunchResult,
) {
    private val lifecycleScope = lifecycleOwner.lifecycleScope
    private var resumeJob: Job? = null

    fun present(
        launchLoading: () -> Unit,
        launchReady: () -> Unit,
    ) {
        if (sheetStateHolder.sheetIsOpen) return

        sheetStateHolder.sheetIsOpen = true
        if (isUpdating.value) {
            awaitingReadyState.isAwaitingReady = true
            launchLoading()
            resumePendingReadyLaunch()
        } else {
            launchReady()
            awaitingReadyState.isAwaitingReady = false
        }
    }

    fun resumePendingReadyLaunch() {
        if (!awaitingReadyState.isAwaitingReady || resumeJob?.isActive == true) return

        resumeJob = lifecycleScope.launch {
            isUpdating.first { isUpdating -> !isUpdating }
            if (!awaitingReadyState.isAwaitingReady) return@launch
            if (!sheetStateHolder.sheetIsOpen) {
                awaitingReadyState.isAwaitingReady = false
                return@launch
            }

            awaitingReadyState.isAwaitingReady =
                launchPendingReady() == ReadyLaunchResult.KeepLoading
        }
    }

    fun close() {
        clearPresentation()
        resumeJob?.cancel()
        resumeJob = null
    }

    private fun clearPresentation() {
        awaitingReadyState.isAwaitingReady = false
        sheetStateHolder.sheetIsOpen = false
    }
}

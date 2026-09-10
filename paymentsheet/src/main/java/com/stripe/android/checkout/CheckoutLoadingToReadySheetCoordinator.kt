package com.stripe.android.checkout

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal interface AwaitingReadyState {
    var isAwaitingReady: Boolean
    var sheetStateVersion: Int?
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
        awaitingReadyState.sheetStateVersion = sheetStateHolder.sheetStateVersion
        try {
            if (isUpdating.value) {
                awaitingReadyState.isAwaitingReady = true
                launchLoading()
                resumePendingReadyLaunch()
            } else {
                launchReady()
                awaitingReadyState.isAwaitingReady = false
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            close()
            throw error
        }
    }

    fun resumePendingReadyLaunch() {
        if (!awaitingReadyState.isAwaitingReady || resumeJob?.isActive == true) return

        resumeJob = lifecycleScope.launch {
            isUpdating.first { isUpdating -> !isUpdating }
            if (!awaitingReadyState.isAwaitingReady) return@launch
            adoptLegacySheetGateIfNeeded()
            if (!ownsSheetGate()) {
                awaitingReadyState.isAwaitingReady = false
                return@launch
            }

            try {
                awaitingReadyState.isAwaitingReady =
                    launchPendingReady() == ReadyLaunchResult.KeepLoading
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                clearPresentation()
                throw error
            }
        }
    }

    fun close() {
        clearPresentation()
        resumeJob?.cancel()
        resumeJob = null
    }

    private fun clearPresentation() {
        awaitingReadyState.isAwaitingReady = false
        val expectedVersion = awaitingReadyState.sheetStateVersion
        val ownsCurrentGate = expectedVersion == sheetStateHolder.sheetStateVersion
        val ownsLegacyGate = expectedVersion == null && sheetStateHolder.sheetStateVersion == 0
        if (sheetStateHolder.sheetIsOpen && (ownsCurrentGate || ownsLegacyGate)) {
            sheetStateHolder.sheetIsOpen = false
        }
        awaitingReadyState.sheetStateVersion = null
    }

    private fun ownsSheetGate(): Boolean {
        return sheetStateHolder.sheetIsOpen &&
            awaitingReadyState.sheetStateVersion == sheetStateHolder.sheetStateVersion
    }

    private fun adoptLegacySheetGateIfNeeded() {
        if (
            awaitingReadyState.sheetStateVersion == null &&
            sheetStateHolder.sheetIsOpen &&
            sheetStateHolder.sheetStateVersion == 0
        ) {
            awaitingReadyState.sheetStateVersion = 0
        }
    }
}

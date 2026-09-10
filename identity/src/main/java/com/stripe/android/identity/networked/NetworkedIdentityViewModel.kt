package com.stripe.android.identity.networked

import androidx.lifecycle.ViewModel

/**
 * Own this ViewModel in the host flow's ViewModelStore. Configuration recreation retains the flow;
 * removing the owner permanently abandons it. Recomposition and backgrounding are not dismissal.
 */
internal class NetworkedIdentityViewModel(
    private val coordinator: NetworkedIdentityCoordinator
) : ViewModel() {
    val state = coordinator.state

    fun submitEmail(email: String) = coordinator.submitEmail(email)

    fun submitOtp(code: String) = coordinator.submitOtp(code)

    fun selectDocument(documentId: String) = coordinator.selectDocument(documentId)

    fun useManualCapture() = coordinator.useManualCapture()

    fun cancel() = coordinator.cancel()

    /** Call for explicit external dismissal, including when the host retains its ViewModelStore. */
    fun abandon() = coordinator.abandon()

    override fun onCleared() {
        coordinator.abandon()
        super.onCleared()
    }
}

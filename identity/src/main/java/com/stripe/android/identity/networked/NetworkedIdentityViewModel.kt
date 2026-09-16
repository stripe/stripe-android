package com.stripe.android.identity.networked

import androidx.lifecycle.ViewModel

/**
 * Own this ViewModel in the host flow's ViewModelStore. Configuration recreation retains the flow;
 * removing the owner permanently abandons it. Recomposition and backgrounding are not dismissal.
 */
internal class NetworkedIdentityViewModel(
    private val coordinator: NetworkedIdentityCoordinator
) : ViewModel() {
    private var hasAppeared = false

    val state = coordinator.state
    val supportsDocumentAttachment = coordinator.supportsDocumentAttachment

    /** The nullable candidate has already passed the UI's exact-match email validation. */
    fun onFirstAppearance(emailAddress: String?) {
        if (hasAppeared) return
        hasAppeared = true
        if (emailAddress != null && state.value == NetworkedIdentityState.CollectEmail) {
            coordinator.submitEmail(emailAddress)
        }
    }

    fun submitEmail(email: String) = coordinator.submitEmail(email)

    fun submitOtp(code: String) = coordinator.submitOtp(code)

    fun resendOtp() = coordinator.resendOtp()

    fun selectDocument(documentId: String) = coordinator.selectDocument(documentId)

    fun continueWithSelectedDocument() = coordinator.continueWithSelectedDocument()

    fun useManualCapture() = coordinator.useManualCapture()

    fun cancel() = coordinator.cancel()

    /** Call for explicit external dismissal, including when the host retains its ViewModelStore. */
    fun abandon() = coordinator.abandon()

    override fun onCleared() {
        coordinator.abandon()
        super.onCleared()
    }
}

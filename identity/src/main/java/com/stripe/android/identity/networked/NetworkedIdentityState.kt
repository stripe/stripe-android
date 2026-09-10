package com.stripe.android.identity.networked

/** Renderable state only: credentials, email input, and OTP input never enter this flow. */
internal sealed interface NetworkedIdentityState {
    data object CollectEmail : NetworkedIdentityState
    data object LookupPending : NetworkedIdentityState
    data object OtpStartPending : NetworkedIdentityState
    data class AwaitingOtp(
        val redactedPhoneNumber: String,
        val invalidCode: Boolean,
        val otpGeneration: Int
    ) : NetworkedIdentityState
    data class OtpConfirmPending(
        val redactedPhoneNumber: String,
        val otpGeneration: Int
    ) : NetworkedIdentityState
    data object ReauthenticationRequired : NetworkedIdentityState
    data object DocumentsPending : NetworkedIdentityState
    data class SelectDocument(
        val documents: List<NetworkedIdentityDocument>,
        val selectedDocumentId: String?
    ) : NetworkedIdentityState
    data class FullCaptureFallback(val reason: NetworkedIdentityFallbackReason) : NetworkedIdentityState
    data object Cancelled : NetworkedIdentityState
}

internal enum class NetworkedIdentityFallbackReason {
    NoLinkAccount,
    NoReusableDocuments,
    Unavailable,
    UserSelectedManualCapture
}

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
    data class OtpResendPending(
        val redactedPhoneNumber: String,
        val otpGeneration: Int
    ) : NetworkedIdentityState
    data object ReauthenticationRequired : NetworkedIdentityState
    data object DocumentsPending : NetworkedIdentityState
    data class SelectDocument(
        val documents: List<NetworkedIdentityDocument>,
        val selectedDocumentId: String?
    ) : NetworkedIdentityState
    data object AttachmentPending : NetworkedIdentityState
    data object SkipPending : NetworkedIdentityState

    /** The host must still process server requirements and normal Identity submission. */
    data object Completed : NetworkedIdentityState
    data class FullCaptureFallback(val reason: NetworkedIdentityFallbackReason) : NetworkedIdentityState
    data object Cancelled : NetworkedIdentityState
}

internal enum class NetworkedIdentityFallbackReason {
    NoLinkAccount,
    NoReusableDocuments,
    Unavailable,
    UserSelectedManualCapture
}

/** Sanitized errors contain neither backend messages nor the original credential-bearing cause. */
internal class NetworkedIdentityActionException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        TokenUnavailable,
        AttachmentFailed,
        SkipFailed,
        UnexpectedSession
    }
}

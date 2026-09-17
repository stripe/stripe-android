package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.VerificationPageData

/** Renderable state only: credentials, email input, and OTP input never enter this flow. */
internal sealed interface NetworkedIdentityState {
    /** No attempt in progress; the Link sheet is hidden. */
    data object Idle : NetworkedIdentityState

    /** Configuring Link or restoring a handed-in session. */
    data object Preparing : NetworkedIdentityState
    data object CollectEmail : NetworkedIdentityState
    data object LookupPending : NetworkedIdentityState

    /** Save only: the email has no Link account yet, so a phone number is needed to sign up. */
    data class CollectPhone(val email: String, val error: String? = null) : NetworkedIdentityState
    data class SignUpPending(val email: String) : NetworkedIdentityState
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
    data class SharingDocument(val document: NetworkedIdentityDocument) : NetworkedIdentityState
    data class DocumentShared(val document: NetworkedIdentityDocument) : NetworkedIdentityState
    data object SavePending : NetworkedIdentityState
    data object Saved : NetworkedIdentityState

    /** Saving failed; the sheet stays open to say so, since there's no capture to fall back to. */
    data class SaveFailed(val details: String?) : NetworkedIdentityState
    data class FullCaptureFallback(val reason: NetworkedIdentityFallbackReason) : NetworkedIdentityState
    data object Cancelled : NetworkedIdentityState
}

internal val NetworkedIdentityState.isSheetVisible: Boolean
    get() = this != NetworkedIdentityState.Idle &&
        this != NetworkedIdentityState.Cancelled &&
        this !is NetworkedIdentityState.FullCaptureFallback

internal enum class NetworkedIdentityFallbackReason {
    NoLinkAccount,
    NoReusableDocuments,
    Unavailable,
    UserSelectedManualCapture
}

internal enum class NetworkedIdentityMode {
    /** Returning user: share a saved ID, started from the intro. */
    Reuse,

    /** Save this verification's ID to Link, started from the success screen. */
    Save,
}

/** How an attempt ended, for the Identity flow to act on. */
internal sealed interface NetworkedIdentityOutcome {
    /** [attached] is the verification after attaching, whose requirements the host continues from. */
    data class DocumentShared(
        val document: NetworkedIdentityDocument,
        val attached: VerificationPageData,
    ) : NetworkedIdentityOutcome
    data object Saved : NetworkedIdentityOutcome
    data class Fallback(val reason: NetworkedIdentityFallbackReason) : NetworkedIdentityOutcome
    data object Cancelled : NetworkedIdentityOutcome
}

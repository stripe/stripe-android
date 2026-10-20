package com.stripe.android.identity.networked

/** Renderable state only: credentials never enter this flow. */
internal sealed interface NetworkedIdentityState {
    /** No attempt in progress; the Link sheet is hidden. */
    data object Idle : NetworkedIdentityState

    /** Configuring Link or restoring a handed-in session; Identity's sheet is hidden meanwhile. */
    data object Preparing : NetworkedIdentityState
    data object DocumentsPending : NetworkedIdentityState
    data class SelectDocument(
        val documents: List<NetworkedIdentityDocument>,
        val selectedDocumentId: String?
    ) : NetworkedIdentityState
    data class SharingDocument(val document: NetworkedIdentityDocument) : NetworkedIdentityState
    data class DocumentShared(val document: NetworkedIdentityDocument) : NetworkedIdentityState
    data object SavePending : NetworkedIdentityState
    data object SavePrepared : NetworkedIdentityState
    data object SkipPending : NetworkedIdentityState

    /** Saving failed; the sheet stays open to say so, since there's no capture to fall back to. */
    data class SaveFailed(val details: String?) : NetworkedIdentityState
    data object FullCaptureFallback : NetworkedIdentityState
    data object Cancelled : NetworkedIdentityState

    /** Link's own screens handle sign-in and sign-up; Identity's sheet is hidden meanwhile. */
    data object LinkAuthentication : NetworkedIdentityState
}

/** Whether Identity's Link sheet shows this state. Link's own screens cover preparing and signing in. */
internal val NetworkedIdentityState.presentsIdentitySheet: Boolean
    get() = isInProgress && this != NetworkedIdentityState.Preparing &&
        this != NetworkedIdentityState.LinkAuthentication

/** Whether an attempt is in progress; it may be on Link's screens rather than Identity's sheet. */
internal val NetworkedIdentityState.isInProgress: Boolean
    get() = this != NetworkedIdentityState.Idle &&
        this != NetworkedIdentityState.Cancelled &&
        this != NetworkedIdentityState.FullCaptureFallback

internal enum class NetworkedIdentityMode {
    /** Returning user: share a saved ID, started from the intro. */
    Reuse,

    /** Save this verification's ID to Link, offered once nothing else is missing, before submitting. */
    Save,
}

/**
 * How an attempt ended, for the Identity flow to act on. Committed writes already reached the host when they
 * happened, so outcomes carry no verification.
 */
internal sealed interface NetworkedIdentityOutcome {
    data object DocumentShared : NetworkedIdentityOutcome
    data object SavePrepared : NetworkedIdentityOutcome
    data object ManualCapture : NetworkedIdentityOutcome
    data object Fallback : NetworkedIdentityOutcome
    data object Cancelled : NetworkedIdentityOutcome
}

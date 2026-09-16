package com.stripe.android.identity.networked

import androidx.annotation.MainThread
import com.stripe.android.core.exception.StripeException
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.uicore.elements.EmailConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Link authentication, saved-document selection, and preview Identity actions. The host remains
 * responsible for processing server requirements and submitting the Identity verification.
 * All actions and request completions run on the main dispatcher supplied by the owner.
 */
@MainThread
internal class NetworkedIdentityCoordinator(
    private val repository: NetworkedIdentityRepository,
    private val documentRequirements: NetworkedIdentityDocumentRequirements,
    private val actions: NetworkedIdentityActions?,
    private val locale: String,
    initialAuthSessionSecrets: List<String>,
    private val currentTimeSeconds: () -> Long,
    dispatcher: CoroutineDispatcher,
    onCancel: () -> Unit,
    onFallback: (NetworkedIdentityFallbackReason) -> Unit,
    onComplete: (Result<VerificationPageData>) -> Unit
) {
    // Deliberately independent of viewModelScope: logout and responses carrying rotated credentials
    // must finish after onCleared. Completing this job on exit lets existing children drain.
    private val requestJob = SupervisorJob()
    private val requestScope = CoroutineScope(requestJob + dispatcher)
    private val mutableState = MutableStateFlow<NetworkedIdentityState>(NetworkedIdentityState.CollectEmail)
    val state = mutableState.asStateFlow()
    val supportsDocumentAttachment = actions != null

    private var credentials: NetworkedIdentityCredentials? = null
    private var authSessionSecrets = initialAuthSessionSecrets.filter { it.isNotBlank() }.distinct()
    private var knownSmsIds = emptySet<String>()
    private var activeSmsId: String? = null
    private var redactedPhoneNumber = ""
    private var otpGeneration = 0
    private var cancelCallback: (() -> Unit)? = onCancel
    private var fallbackCallback: ((NetworkedIdentityFallbackReason) -> Unit)? = onFallback
    private var completionCallback: ((Result<VerificationPageData>) -> Unit)? = onComplete
    private var callbackDelivered = false

    fun submitEmail(email: String) {
        if (state.value != NetworkedIdentityState.CollectEmail &&
            state.value != NetworkedIdentityState.ReauthenticationRequired
        ) {
            return
        }
        val normalizedEmail = email.trim()
        if (!EmailConfig.PATTERN.matcher(normalizedEmail).matches()) return

        val requestSecrets = authSessionSecrets
        mutableState.value = NetworkedIdentityState.LookupPending
        requestScope.launch {
            if (linkRequestsObsolete) return@launch
            val result = repository.lookup(normalizedEmail, requestSecrets)
            if (linkRequestsObsolete) {
                val found = result.getOrNull() as? NetworkedIdentityLookup.Found
                if (found != null) {
                    logOut(
                        NetworkedIdentityCredentials(found.publishableKey, found.session.clientSecret),
                        requestSecrets.appending(found.authSessionClientSecret)
                    )
                }
                return@launch
            }
            result.fold(
                onSuccess = { lookup ->
                    when (lookup) {
                        is NetworkedIdentityLookup.Found -> {
                            onAccountFound(lookup)
                        }
                        is NetworkedIdentityLookup.NotFound -> fallBack(NetworkedIdentityFallbackReason.NoLinkAccount)
                    }
                },
                onFailure = { fallBack(NetworkedIdentityFallbackReason.Unavailable) }
            )
        }
    }

    fun submitOtp(code: String) {
        if (state.value !is NetworkedIdentityState.AwaitingOtp ||
            code.length != OTP_LENGTH || code.any { it !in '0'..'9' }
        ) {
            return
        }
        val requestCredentials = credentials ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable)
        val requestSecrets = authSessionSecrets
        mutableState.value = NetworkedIdentityState.OtpConfirmPending(redactedPhoneNumber, otpGeneration)
        requestScope.launch {
            if (linkRequestsObsolete) return@launch
            val result = repository.confirmVerification(requestCredentials, code, requestSecrets)
            if (linkRequestsObsolete) {
                logOutLateResponse(result, requestCredentials, requestSecrets)
                return@launch
            }
            result.fold(
                onSuccess = success@{ response ->
                    if (!updateSession(response)) {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                        return@success
                    }
                    if (response.session.verificationSessions.any {
                            it.id == activeSmsId && activeSmsId != null &&
                                it.type == NetworkedIdentityVerificationType.SMS &&
                                it.state == NetworkedIdentityVerificationState.VERIFIED
                        }
                    ) {
                        activeSmsId = null
                        loadDocuments()
                    } else {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                    }
                },
                onFailure = ::handleConfirmationError
            )
        }
    }

    fun resendOtp() {
        if (state.value !is NetworkedIdentityState.AwaitingOtp) return
        startVerification(isResendingSmsCode = true)
    }

    fun selectDocument(documentId: String) {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        if (selection.documents.none { it.id == documentId }) return
        mutableState.value = selection.copy(selectedDocumentId = documentId)
        // Selection does not attach the document. The user must explicitly continue.
    }

    fun continueWithSelectedDocument() {
        val identityActions = actions ?: return
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        val selectedDocument = selection.documents.firstOrNull { it.id == selection.selectedDocumentId } ?: return
        val requestCredentials = credentials
        if (requestCredentials == null ||
            documentRequirements.filter(listOf(selectedDocument), currentTimeSeconds()).isEmpty()
        ) {
            fallBack(NetworkedIdentityFallbackReason.Unavailable)
            return
        }
        mutableState.value = NetworkedIdentityState.AttachmentPending
        requestScope.launch {
            if (state.value != NetworkedIdentityState.AttachmentPending) return@launch
            // Mint only after explicit Continue and redeem once. Never retain tokens in screen state.
            val token = repository.createAssociationToken(requestCredentials, selectedDocument.id)
                .getOrNull()?.associationToken
            if (state.value != NetworkedIdentityState.AttachmentPending) return@launch
            if (token.isNullOrBlank()) {
                finishAction(actionFailure(NetworkedIdentityActionException.Reason.TokenUnavailable))
                return@launch
            }
            val result = identityActions.attachDocument(token)
            if (state.value != NetworkedIdentityState.AttachmentPending) return@launch
            if (result.exceptionOrNull()?.consumerErrorCode == NETWORKED_IDENTITY_UNAVAILABLE) {
                fallBack(NetworkedIdentityFallbackReason.Unavailable)
            } else {
                finishAction(result.sanitized(NetworkedIdentityActionException.Reason.AttachmentFailed))
            }
            // #TODO - Networked Identity: Define approved fresh-token recovery and ambiguous
            // redemption errors before adding recovery. Never automatically replay an association token.
        }
    }

    fun useManualCapture() {
        if (flowEnded || state.value == NetworkedIdentityState.AttachmentPending ||
            state.value == NetworkedIdentityState.SkipPending
        ) {
            return
        }
        val identityActions = actions
        if (identityActions == null) {
            fallBack(NetworkedIdentityFallbackReason.UserSelectedManualCapture)
            return
        }
        mutableState.value = NetworkedIdentityState.SkipPending
        requestScope.launch {
            if (state.value != NetworkedIdentityState.SkipPending) return@launch
            val result = identityActions.skip()
            if (state.value != NetworkedIdentityState.SkipPending) return@launch
            finishAction(result.sanitized(NetworkedIdentityActionException.Reason.SkipFailed))
        }
    }

    fun cancel() = endAsCancelled(notifyHost = true)

    fun abandon() = endAsCancelled(notifyHost = false)

    private fun onAccountFound(lookup: NetworkedIdentityLookup.Found) {
        if (lookup.publishableKey.isBlank() || lookup.session.clientSecret.isBlank()) {
            fallBack(NetworkedIdentityFallbackReason.Unavailable)
            return
        }
        credentials = NetworkedIdentityCredentials(lookup.publishableKey, lookup.session.clientSecret)
        authSessionSecrets = authSessionSecrets.appending(lookup.authSessionClientSecret)
        redactedPhoneNumber = lookup.session.redactedFormattedPhoneNumber
        knownSmsIds = lookup.session.smsIds()
        startVerification(isResendingSmsCode = false)
    }

    private fun startVerification(isResendingSmsCode: Boolean) {
        val requestCredentials = credentials ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable)
        val requestSecrets = authSessionSecrets
        val previousSmsIds = knownSmsIds
        val resendSmsId = activeSmsId.takeIf { isResendingSmsCode }
        activeSmsId = null
        mutableState.value = if (isResendingSmsCode) {
            otpGeneration += 1
            NetworkedIdentityState.OtpResendPending(redactedPhoneNumber, otpGeneration)
        } else {
            NetworkedIdentityState.OtpStartPending
        }
        requestScope.launch {
            if (linkRequestsObsolete) return@launch
            val result = repository.startVerification(
                credentials = requestCredentials,
                locale = locale,
                accountPhoneNumber = null,
                isResendingSmsCode = isResendingSmsCode,
                authSessionSecrets = requestSecrets
            )
            if (linkRequestsObsolete) {
                logOutLateResponse(result, requestCredentials, requestSecrets)
                return@launch
            }
            result.fold(
                onSuccess = success@{ response ->
                    if (!updateSession(response)) {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                        return@success
                    }
                    val startedSmsId = startedSmsId(response.session, previousSmsIds, resendSmsId)
                    knownSmsIds = knownSmsIds + response.session.smsIds()
                    if (startedSmsId != null) {
                        activeSmsId = startedSmsId
                        if (!isResendingSmsCode) otpGeneration += 1
                        mutableState.value = awaitingOtp(invalidCode = false)
                    } else {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                    }
                },
                onFailure = { error ->
                    if (error.consumerErrorCode == SESSION_EXPIRED) {
                        requireReauthentication()
                    } else {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                    }
                }
            )
        }
    }

    private fun startedSmsId(
        session: NetworkedIdentityConsumerSession,
        previousSmsIds: Set<String>,
        resendSmsId: String?
    ): String? {
        val startedSessions = session.verificationSessions.filter {
            it.type == NetworkedIdentityVerificationType.SMS &&
                it.state == NetworkedIdentityVerificationState.STARTED && !it.id.isNullOrBlank()
        }
        val freshSessions = startedSessions.filter { it.id !in previousSmsIds }
        val eligibleSessions = if (freshSessions.isEmpty() && resendSmsId != null) {
            startedSessions.filter { it.id == resendSmsId }
        } else {
            freshSessions
        }
        // #TODO - Networked Identity: Verify resend's retained/replacement SMS ID behavior against the
        // web flow and an NI-enabled backend. Initial and expired-code starts still require a new ID.
        return eligibleSessions.singleOrNull()?.id
    }

    private fun loadDocuments() {
        val requestCredentials = credentials ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable)
        mutableState.value = NetworkedIdentityState.DocumentsPending
        requestScope.launch {
            if (linkRequestsObsolete) return@launch
            val result = repository.listDocuments(requestCredentials)
            if (linkRequestsObsolete) return@launch
            result.fold(
                onSuccess = { documents ->
                    val eligible = documentRequirements.filter(documents, currentTimeSeconds())
                    if (eligible.isEmpty()) {
                        fallBack(NetworkedIdentityFallbackReason.NoReusableDocuments)
                    } else {
                        mutableState.value = NetworkedIdentityState.SelectDocument(eligible, null)
                    }
                },
                onFailure = { fallBack(NetworkedIdentityFallbackReason.Unavailable) }
            )
        }
    }

    private fun handleConfirmationError(error: Throwable) {
        when (error.consumerErrorCode) {
            INVALID_CODE -> mutableState.value = awaitingOtp(invalidCode = true)
            VERIFICATION_EXPIRED -> startVerification(isResendingSmsCode = false)
            SESSION_EXPIRED -> requireReauthentication()
            else -> fallBack(NetworkedIdentityFallbackReason.Unavailable)
        }
    }

    private fun updateSession(response: NetworkedIdentitySessionResponse): Boolean {
        if (response.session.clientSecret.isBlank()) return false
        credentials = credentials?.copy(sessionClientSecret = response.session.clientSecret)
        authSessionSecrets = authSessionSecrets.appending(response.authSessionClientSecret)
        redactedPhoneNumber = response.session.redactedFormattedPhoneNumber
        return true
    }

    private fun requireReauthentication() {
        clearConsumer()
        mutableState.value = NetworkedIdentityState.ReauthenticationRequired
    }

    private fun fallBack(reason: NetworkedIdentityFallbackReason) {
        if (flowEnded) return
        clearAndLogOut()
        mutableState.value = NetworkedIdentityState.FullCaptureFallback(reason)
        requestScope.launch {
            // A host observing the terminal state can cancel/dismiss before navigation is dispatched.
            yield()
            if (state.value is NetworkedIdentityState.FullCaptureFallback && !callbackDelivered) {
                deliverCallback { fallbackCallback?.invoke(reason) }
            }
        }
        requestJob.complete()
        // Automatic unavailability is not an explicit skip. Only useManualCapture persists that
        // choice through Identity actions; cancellation never records consent or deletes documents.
        // #TODO - Networked Identity: Reconcile pre-capture versus post-capture save opt-in before
        // orchestrating save_association_token and prepare_document_save. Backend saving is asynchronous.
    }

    private fun finishAction(result: Result<VerificationPageData>) {
        val checkedResult = if (result.getOrNull()?.let { it.id != actions?.verificationSessionId } == true) {
            actionFailure(NetworkedIdentityActionException.Reason.UnexpectedSession)
        } else {
            result
        }
        clearAndLogOut()
        mutableState.value = NetworkedIdentityState.Completed
        if (!callbackDelivered) deliverCallback { completionCallback?.invoke(checkedResult) }
        requestJob.complete()
    }

    private fun endAsCancelled(notifyHost: Boolean) {
        if (state.value == NetworkedIdentityState.Cancelled || state.value == NetworkedIdentityState.Completed) return
        clearAndLogOut()
        mutableState.value = NetworkedIdentityState.Cancelled
        if (notifyHost && !callbackDelivered) {
            deliverCallback { cancelCallback?.invoke() }
        } else {
            releaseCallbacks()
        }
        requestJob.complete()
    }

    private fun clearAndLogOut() {
        val logoutCredentials = credentials
        val logoutSecrets = authSessionSecrets
        clearConsumer()
        authSessionSecrets = emptyList()
        if (logoutCredentials != null) {
            requestScope.launch { logOut(logoutCredentials, logoutSecrets) }
        }
    }

    private fun clearConsumer() {
        credentials = null
        redactedPhoneNumber = ""
        activeSmsId = null
        knownSmsIds = emptySet()
    }

    private fun deliverCallback(block: () -> Unit) {
        callbackDelivered = true
        try {
            block()
        } finally {
            releaseCallbacks()
        }
    }

    private fun releaseCallbacks() {
        cancelCallback = null
        fallbackCallback = null
        completionCallback = null
    }

    private fun awaitingOtp(invalidCode: Boolean) = NetworkedIdentityState.AwaitingOtp(
        redactedPhoneNumber = redactedPhoneNumber,
        invalidCode = invalidCode,
        otpGeneration = otpGeneration
    )

    private suspend fun logOutLateResponse(
        result: Result<NetworkedIdentitySessionResponse>,
        requestCredentials: NetworkedIdentityCredentials,
        requestSecrets: List<String>
    ) {
        result.getOrNull()?.let { response ->
            logOut(
                requestCredentials.copy(sessionClientSecret = response.session.clientSecret),
                requestSecrets.appending(response.authSessionClientSecret)
            )
        }
    }

    private suspend fun logOut(credentials: NetworkedIdentityCredentials, secrets: List<String>) {
        // Best effort and intentionally silent: errors must not log credential-bearing responses.
        runCatching { repository.logout(credentials, secrets) }
    }

    private val flowEnded: Boolean
        get() = state.value == NetworkedIdentityState.Cancelled ||
            state.value is NetworkedIdentityState.FullCaptureFallback ||
            state.value == NetworkedIdentityState.Completed

    private val linkRequestsObsolete: Boolean
        get() = flowEnded || state.value == NetworkedIdentityState.SkipPending

    private fun actionFailure(reason: NetworkedIdentityActionException.Reason): Result<VerificationPageData> =
        Result.failure(NetworkedIdentityActionException(reason))

    private fun Result<VerificationPageData>.sanitized(
        reason: NetworkedIdentityActionException.Reason
    ): Result<VerificationPageData> = if (isFailure) actionFailure(reason) else this

    private val Throwable.consumerErrorCode: String?
        get() = (this as? StripeException)?.stripeError?.code

    private fun NetworkedIdentityConsumerSession.smsIds(): Set<String> = verificationSessions
        .filter { it.type == NetworkedIdentityVerificationType.SMS }
        .mapNotNull { it.id?.takeIf(String::isNotBlank) }
        .toSet()

    private fun List<String>.appending(secret: String?): List<String> =
        if (secret.isNullOrBlank() || secret in this) this else this + secret

    private companion object {
        const val OTP_LENGTH = 6
        const val INVALID_CODE = "consumer_verification_code_invalid"
        const val VERIFICATION_EXPIRED = "consumer_verification_expired"
        const val SESSION_EXPIRED = "consumer_session_expired"
        const val NETWORKED_IDENTITY_UNAVAILABLE = "networked_identity_unavailable"
    }
}

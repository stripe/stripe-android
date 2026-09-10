package com.stripe.android.identity.networked

import androidx.annotation.MainThread
import com.stripe.android.core.exception.StripeException
import com.stripe.android.uicore.elements.EmailConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Standalone preparation for document reuse. Selecting a document is not Identity verification.
 * All actions and request completions run on the main dispatcher supplied by the owner.
 */
@MainThread
internal class NetworkedIdentityCoordinator(
    private val repository: NetworkedIdentityRepository,
    private val documentRequirements: NetworkedIdentityDocumentRequirements,
    private val locale: String,
    initialAuthSessionSecrets: List<String>,
    private val currentTimeSeconds: () -> Long,
    dispatcher: CoroutineDispatcher,
    onCancel: () -> Unit,
    onFallback: (NetworkedIdentityFallbackReason) -> Unit
) {
    // Deliberately independent of viewModelScope: logout and responses carrying rotated credentials
    // must finish after onCleared. Completing this job on exit lets existing children drain.
    private val requestJob = SupervisorJob()
    private val requestScope = CoroutineScope(requestJob + dispatcher)
    private val mutableState = MutableStateFlow<NetworkedIdentityState>(NetworkedIdentityState.CollectEmail)
    val state = mutableState.asStateFlow()

    private var credentials: NetworkedIdentityCredentials? = null
    private var authSessionSecrets = initialAuthSessionSecrets.filter { it.isNotBlank() }.distinct()
    private var knownSmsIds = emptySet<String>()
    private var activeSmsId: String? = null
    private var redactedPhoneNumber = ""
    private var otpGeneration = 0
    private var cancelCallback: (() -> Unit)? = onCancel
    private var fallbackCallback: ((NetworkedIdentityFallbackReason) -> Unit)? = onFallback
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
            if (flowEnded) return@launch
            val result = repository.lookup(normalizedEmail, requestSecrets)
            if (flowEnded) {
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
            if (flowEnded) return@launch
            val result = repository.confirmVerification(requestCredentials, code, requestSecrets)
            if (flowEnded) {
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

    fun selectDocument(documentId: String) {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        if (selection.documents.none { it.id == documentId }) return
        mutableState.value = selection.copy(selectedDocumentId = documentId)
        // #TODO - Networked Identity: Clone/attach endpoint, association-token linkage/lifetime,
        // auth, response, errors, and retry/idempotency rules are required before submitting reuse.
    }

    fun useManualCapture() {
        if (!flowEnded) fallBack(NetworkedIdentityFallbackReason.UserSelectedManualCapture)
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
        startFreshVerification()
    }

    private fun startFreshVerification() {
        val requestCredentials = credentials ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable)
        val requestSecrets = authSessionSecrets
        val previousSmsIds = knownSmsIds
        activeSmsId = null
        mutableState.value = NetworkedIdentityState.OtpStartPending
        requestScope.launch {
            if (flowEnded) return@launch
            val result = repository.startVerification(
                credentials = requestCredentials,
                locale = locale,
                accountPhoneNumber = null,
                authSessionSecrets = requestSecrets
            )
            if (flowEnded) {
                logOutLateResponse(result, requestCredentials, requestSecrets)
                return@launch
            }
            result.fold(
                onSuccess = success@{ response ->
                    if (!updateSession(response)) {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable)
                        return@success
                    }
                    val freshSessions = response.session.verificationSessions.filter {
                        it.type == NetworkedIdentityVerificationType.SMS &&
                            it.state == NetworkedIdentityVerificationState.STARTED &&
                            !it.id.isNullOrBlank() && it.id !in previousSmsIds
                    }
                    knownSmsIds = knownSmsIds + response.session.smsIds()
                    // #TODO - Networked Identity: Confirm fresh SMS ID semantics for first start,
                    // expiry restart, and resend. Recycled or ambiguous IDs intentionally fail closed.
                    if (freshSessions.size == 1) {
                        activeSmsId = freshSessions.single().id
                        otpGeneration += 1
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

    // #TODO - Networked Identity: Explicit resend needs confirmed NI request parameters and SMS ID
    // replacement semantics; the existing Link is_resend_sms_code precedent is not that guarantee.

    private fun loadDocuments() {
        val requestCredentials = credentials ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable)
        mutableState.value = NetworkedIdentityState.DocumentsPending
        requestScope.launch {
            if (flowEnded) return@launch
            val result = repository.listDocuments(requestCredentials)
            if (flowEnded) return@launch
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
            VERIFICATION_EXPIRED -> startFreshVerification()
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
        // #TODO - Networked Identity: Dedicated skip/manual endpoint and timing remain unspecified.
        // Link logout is only local-session cleanup, not a save-consent mutation or document deletion.
    }

    private fun endAsCancelled(notifyHost: Boolean) {
        if (state.value == NetworkedIdentityState.Cancelled) return
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
            state.value is NetworkedIdentityState.FullCaptureFallback

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
    }
}

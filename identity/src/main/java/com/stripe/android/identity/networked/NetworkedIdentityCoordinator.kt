package com.stripe.android.identity.networked

import androidx.annotation.MainThread
import com.stripe.android.core.exception.StripeException
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.uicore.elements.EmailConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Networked Identity on top of Link's login. Nothing starts or advances without a user action:
 * [startReuse] from the intro, [startSave] from the success screen, and sharing or continuing
 * always need a tap. A handed-in Link session or a known email only lets the flow skip the email or
 * one-time code steps. All actions and request completions run on the dispatcher supplied by the owner.
 */
@MainThread
@Suppress("TooManyFunctions")
internal class NetworkedIdentityCoordinator(
    private val linkSession: NetworkedIdentityLinkSession,
    private val repository: NetworkedIdentityRepository,
    private val actions: NetworkedIdentityActions,
    private val documentRequirements: NetworkedIdentityDocumentRequirements,
    private val config: NetworkedIdentityConfig,
    private val handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
    private val merchantDisplayName: String,
    private val currentTimeSeconds: () -> Long,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val mutableState = MutableStateFlow<NetworkedIdentityState>(NetworkedIdentityState.Idle)
    val state: StateFlow<NetworkedIdentityState> = mutableState.asStateFlow()

    private val mutableMode = MutableStateFlow(NetworkedIdentityMode.Reuse)
    val mode: StateFlow<NetworkedIdentityMode> = mutableMode.asStateFlow()

    private val mutableSaved = MutableStateFlow(false)

    /** Whether this verification's ID was saved to Link. */
    val saved: StateFlow<Boolean> = mutableSaved.asStateFlow()

    private val mutableEntry = MutableStateFlow(NetworkedIdentityEntry.initial(config, handoff))

    /** What the intro and success screens offer; [refreshEntry] fills in the lookup result. */
    val entry: StateFlow<NetworkedIdentityEntry> = mutableEntry.asStateFlow()

    private val mutableShared = MutableStateFlow(false)

    /** Whether a saved ID was shared in this verification, so the success screen doesn't offer saving it. */
    val shared: StateFlow<Boolean> = mutableShared.asStateFlow()

    private val outcomeChannel = Channel<NetworkedIdentityOutcome>(Channel.UNLIMITED)
    val outcomes: Flow<NetworkedIdentityOutcome> = outcomeChannel.receiveAsFlow()

    /** Identifies the current attempt. Responses from a cancelled or restarted attempt are ignored. */
    private var attempt = 0
    private var configuration: Deferred<Boolean>? = null
    private var account: NetworkedIdentityLinkAccount? = null
    private var attached: VerificationPageData? = null
    private var otpGeneration = 0

    fun startReuse() = start(NetworkedIdentityMode.Reuse)

    fun startSave() = start(NetworkedIdentityMode.Save)

    /**
     * Checks whether the provided email has a Link account, without sending a code or opening the sheet.
     * A handed-in session already names its account, and without an email there's nothing to check.
     */
    fun refreshEntry() {
        val email = config.merchantEmail
        val publishableKey = config.merchantPublishableKey
        if (handoff != null || email.isNullOrBlank() || publishableKey.isNullOrBlank()) return
        if (state.value.isSheetVisible) return
        scope.launch {
            if (!ensureConfigured(publishableKey)) return@launch
            val found = linkSession.lookup(email).getOrNull()
            mutableEntry.value = mutableEntry.value.copy(accountEmail = found?.email)
        }
    }

    fun submitEmail(email: String) {
        val current = state.value
        if (current != NetworkedIdentityState.CollectEmail &&
            current != NetworkedIdentityState.ReauthenticationRequired
        ) {
            return
        }
        val normalizedEmail = email.trim()
        if (!EmailConfig.PATTERN.matcher(normalizedEmail).matches()) return
        lookup(attempt, normalizedEmail)
    }

    fun submitPhone(phoneNumber: String, country: String) {
        val current = state.value as? NetworkedIdentityState.CollectPhone ?: return
        if (phoneNumber.isBlank() || country.isBlank()) return
        val id = attempt
        mutableState.value = NetworkedIdentityState.SignUpPending(current.email)
        scope.launch {
            val result = linkSession.signUp(
                email = current.email,
                phoneNumber = phoneNumber,
                country = country,
                name = null,
            )
            if (id != attempt) return@launch
            result.fold(
                onSuccess = { onAccount(id, it) },
                onFailure = { error ->
                    // Staying on the step keeps the reason visible; dismissing the sheet would hide it.
                    mutableState.value = NetworkedIdentityState.CollectPhone(
                        email = current.email,
                        error = error.message ?: "Could not create the Link account.",
                    )
                },
            )
        }
    }

    fun submitOtp(code: String) {
        if (state.value !is NetworkedIdentityState.AwaitingOtp ||
            code.length != OTP_LENGTH || code.any { it !in '0'..'9' }
        ) {
            return
        }
        val id = attempt
        mutableState.value = NetworkedIdentityState.OtpConfirmPending(redactedPhoneNumber, otpGeneration)
        scope.launch {
            val result = linkSession.confirmVerification(code)
            if (id != attempt) return@launch
            result.fold(
                onSuccess = { verified ->
                    account = verified
                    proceedAuthenticated(id)
                },
                onFailure = { handleConfirmationError(id, it) },
            )
        }
    }

    fun resendOtp() {
        if (state.value !is NetworkedIdentityState.AwaitingOtp) return
        sendCode(attempt, isResend = true)
    }

    fun selectDocument(documentId: String) {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        if (selection.documents.none { it.id == documentId }) return
        mutableState.value = selection.copy(selectedDocumentId = documentId)
    }

    fun shareSelectedDocument() {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        val document = selection.documents.firstOrNull { it.id == selection.selectedDocumentId } ?: return
        val credentials = account?.credentials
            ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable, MissingConsumerSession())
        val id = attempt
        mutableState.value = NetworkedIdentityState.SharingDocument(document)
        scope.launch {
            // Minted only after the explicit tap and redeemed once: association tokens are single-use.
            val tokenResult = repository.createAssociationToken(credentials, document.id)
            val token = tokenResult.getOrNull()?.associationToken
            if (id != attempt) return@launch
            if (token.isNullOrBlank()) {
                return@launch fallBack(NetworkedIdentityFallbackReason.Unavailable, tokenResult.exceptionOrNull())
            }
            val attached = actions.attachDocument(token)
            if (id != attempt) return@launch
            // #TODO - Networked Identity [NI-Contract]: recovery for ambiguous attach failures. Never replay a token.
            if (attached.isSuccess) {
                this@NetworkedIdentityCoordinator.attached = attached.getOrNull()
                mutableState.value = NetworkedIdentityState.DocumentShared(document)
            } else {
                fallBack(NetworkedIdentityFallbackReason.Unavailable, attached.exceptionOrNull())
            }
        }
    }

    fun continueAfterSuccess() {
        val outcome = when (val current = state.value) {
            is NetworkedIdentityState.DocumentShared -> {
                mutableShared.value = true
                NetworkedIdentityOutcome.DocumentShared(current.document, attached ?: return)
            }
            NetworkedIdentityState.Saved -> NetworkedIdentityOutcome.Saved
            else -> return
        }
        attempt += 1
        mutableState.value = NetworkedIdentityState.Idle
        outcomeChannel.trySend(outcome)
    }

    fun useManualCapture() {
        if (!state.value.isSheetVisible) return
        fallBack(NetworkedIdentityFallbackReason.UserSelectedManualCapture, error = null)
    }

    fun cancel() {
        if (!state.value.isSheetVisible) return
        attempt += 1
        mutableState.value = NetworkedIdentityState.Cancelled
        outcomeChannel.trySend(NetworkedIdentityOutcome.Cancelled)
    }

    /** Permanent owner removal: stops all work without reporting an outcome. */
    fun abandon() {
        attempt += 1
        scope.cancel()
        mutableState.value = NetworkedIdentityState.Cancelled
    }

    private fun start(newMode: NetworkedIdentityMode) {
        if (state.value.isSheetVisible) return
        val id = ++attempt
        mutableMode.value = newMode
        account = null
        val publishableKey = config.merchantPublishableKey
        if (publishableKey.isNullOrBlank()) {
            fallBack(NetworkedIdentityFallbackReason.Unavailable, IllegalStateException("No merchant publishable key."))
            return
        }
        mutableState.value = NetworkedIdentityState.Preparing
        scope.launch {
            val ready = ensureConfigured(publishableKey)
            if (id != attempt) return@launch
            if (!ready) {
                return@launch fallBack(
                    NetworkedIdentityFallbackReason.Unavailable,
                    IllegalStateException("Link couldn't be configured."),
                )
            }
            val handedIn = handoff
            if (handedIn == null) {
                continueWithEmail(id, knownEmail = config.merchantEmail)
                return@launch
            }
            val restored = linkSession.restore(
                NetworkedIdentityCredentials(
                    publishableKey = handedIn.consumerPublishableKey,
                    sessionClientSecret = handedIn.consumerSessionClientSecret,
                )
            )
            if (id != attempt) return@launch
            restored.fold(
                onSuccess = { onAccount(id, it) },
                // An expired or invalid handed-in session only loses the shortcut.
                onFailure = { continueWithEmail(id, knownEmail = handedIn.email) },
            )
        }
    }

    /** Configures Link once; concurrent callers share the same attempt, and a failure allows a retry. */
    private suspend fun ensureConfigured(publishableKey: String): Boolean {
        val pending = configuration
            ?: scope.async { linkSession.configure(publishableKey, merchantDisplayName).isSuccess }
                .also { configuration = it }
        val ready = pending.await()
        if (!ready) configuration = null
        return ready
    }

    private fun continueWithEmail(id: Int, knownEmail: String?) {
        if (knownEmail.isNullOrBlank()) {
            mutableState.value = NetworkedIdentityState.CollectEmail
        } else {
            lookup(id, knownEmail)
        }
    }

    private fun lookup(id: Int, email: String) {
        mutableState.value = NetworkedIdentityState.LookupPending
        scope.launch {
            val result = linkSession.lookup(email)
            if (id != attempt) return@launch
            result.fold(
                onSuccess = { found ->
                    when {
                        found != null -> onAccount(id, found)
                        mode.value == NetworkedIdentityMode.Save ->
                            mutableState.value = NetworkedIdentityState.CollectPhone(email)
                        else -> fallBack(NetworkedIdentityFallbackReason.NoLinkAccount, error = null)
                    }
                },
                onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
            )
        }
    }

    private fun onAccount(id: Int, found: NetworkedIdentityLinkAccount) {
        account = found
        if (found.isVerified) {
            proceedAuthenticated(id)
        } else {
            sendCode(id, isResend = false)
        }
    }

    private fun sendCode(id: Int, isResend: Boolean) {
        mutableState.value = NetworkedIdentityState.OtpStartPending
        scope.launch {
            val result = linkSession.startVerification(isResend)
            if (id != attempt) return@launch
            result.fold(
                onSuccess = { updated ->
                    account = updated
                    otpGeneration += 1
                    mutableState.value = awaitingOtp(invalidCode = false)
                },
                onFailure = { error ->
                    if (error.consumerErrorCode == SESSION_EXPIRED) {
                        requireReauthentication()
                    } else {
                        fallBack(NetworkedIdentityFallbackReason.Unavailable, error)
                    }
                },
            )
        }
    }

    private fun handleConfirmationError(id: Int, error: Throwable) {
        when (error.consumerErrorCode) {
            INVALID_CODE -> mutableState.value = awaitingOtp(invalidCode = true)
            VERIFICATION_EXPIRED -> sendCode(id, isResend = false)
            SESSION_EXPIRED -> requireReauthentication()
            else -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error)
        }
    }

    private fun proceedAuthenticated(id: Int) {
        val credentials = account?.credentials
            ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable, MissingConsumerSession())
        when (mode.value) {
            NetworkedIdentityMode.Reuse -> loadDocuments(id, credentials)
            NetworkedIdentityMode.Save -> recordSaveConsent(id, credentials)
        }
    }

    private fun loadDocuments(id: Int, credentials: NetworkedIdentityCredentials) {
        mutableState.value = NetworkedIdentityState.DocumentsPending
        scope.launch {
            val result = repository.listDocuments(credentials)
            if (id != attempt) return@launch
            result.fold(
                onSuccess = { documents ->
                    val eligible = documentRequirements.filter(documents, currentTimeSeconds())
                    if (eligible.isEmpty()) {
                        fallBack(NetworkedIdentityFallbackReason.NoReusableDocuments, error = null)
                    } else {
                        // Sharing always needs an explicit tap; a single document is only preselected.
                        mutableState.value = NetworkedIdentityState.SelectDocument(
                            documents = eligible,
                            selectedDocumentId = eligible.singleOrNull()?.id,
                        )
                    }
                },
                onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
            )
        }
    }

    private fun recordSaveConsent(id: Int, credentials: NetworkedIdentityCredentials) {
        mutableState.value = NetworkedIdentityState.SavePending
        scope.launch {
            // #TODO - Networked Identity [NI-Contract]: confirm prepare_document_save is accepted after the
            // verification is submitted; the design hasn't settled whether saving is offered before or after capture.
            val result = repository.createSaveAssociationToken(credentials, actions.verificationSessionId)
                .mapCatching { token -> actions.prepareDocumentSave(token.associationToken).getOrThrow() }
            if (id != attempt) return@launch
            if (result.isSuccess) {
                mutableSaved.value = true
                val savedEmail = account?.email ?: mutableEntry.value.accountEmail
                mutableEntry.value = mutableEntry.value.copy(accountEmail = savedEmail)
                mutableState.value = NetworkedIdentityState.Saved
            } else {
                fallBack(NetworkedIdentityFallbackReason.Unavailable, result.exceptionOrNull())
            }
        }
    }

    private fun requireReauthentication() {
        account = null
        mutableState.value = NetworkedIdentityState.ReauthenticationRequired
    }

    private fun fallBack(reason: NetworkedIdentityFallbackReason, error: Throwable?) {
        val current = state.value
        if (current == NetworkedIdentityState.Cancelled ||
            current is NetworkedIdentityState.FullCaptureFallback ||
            current is NetworkedIdentityState.SaveFailed
        ) {
            return
        }
        attempt += 1
        // Saving has no capture to fall back to: keep the sheet open and say what went wrong.
        if (mode.value == NetworkedIdentityMode.Save) {
            mutableState.value = NetworkedIdentityState.SaveFailed(error?.details)
            return
        }
        mutableState.value = NetworkedIdentityState.FullCaptureFallback(reason)
        // Only the user's explicit choice is persisted; automatic unavailability isn't a skip. The host
        // continues to capture either way, so a failed skip only loses the recorded choice.
        if (reason == NetworkedIdentityFallbackReason.UserSelectedManualCapture) scope.launch { actions.skip() }
        outcomeChannel.trySend(NetworkedIdentityOutcome.Fallback(reason))
    }

    // The Link session is never logged out: a handed-in session belongs to the module that started it,
    // and the session is only a convenience for later verifications.

    private val redactedPhoneNumber: String
        get() = account?.redactedPhoneNumber.orEmpty()

    private fun awaitingOtp(invalidCode: Boolean) = NetworkedIdentityState.AwaitingOtp(
        redactedPhoneNumber = redactedPhoneNumber,
        invalidCode = invalidCode,
        otpGeneration = otpGeneration,
    )

    private val Throwable.details: String
        get() = (this as? StripeException)?.stripeError?.code?.let { code -> "${message.orEmpty()} ($code)".trim() }
            ?: message
            ?: javaClass.simpleName

    private class MissingConsumerSession : IllegalStateException("Link returned no consumer session.")

    private val Throwable.consumerErrorCode: String?
        get() = (this as? StripeException)?.stripeError?.code

    private companion object {
        const val OTP_LENGTH = 6
        const val INVALID_CODE = "consumer_verification_code_invalid"
        const val VERIFICATION_EXPIRED = "consumer_verification_expired"
        const val SESSION_EXPIRED = "consumer_session_expired"
    }
}

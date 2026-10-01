package com.stripe.android.identity.networked

import androidx.annotation.MainThread
import com.stripe.android.core.exception.StripeException
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

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

    private val route = MutableStateFlow(config.route)
    private val accountEmail = MutableStateFlow<String?>(null)

    /** Whether this verification's ID is prepared to be saved to Link. */
    val hasPreparedSave: StateFlow<Boolean> = route.mapAsStateFlow { it == NetworkedIdentityRoute.ResumeSave }

    /** What the intro and success screens offer; [lookUpProvidedAccountEmail] fills in the account. */
    val entry: StateFlow<NetworkedIdentityEntry> = combineAsStateFlow(route, accountEmail) { route, email ->
        NetworkedIdentityEntry(config = config, handoff = handoff, route = route, accountEmail = email)
    }

    /** Whether Identity's sheet stays hidden until Link's own screens have authenticated the user. */
    val usesLinkUI: Boolean
        get() = config.usesLinkUI

    /** Receives every verification a Networked Identity action committed, so the host's requirements stay current. */
    var onVerificationUpdate: ((VerificationPageData) -> Unit)? = null

    private val outcomeChannel = Channel<NetworkedIdentityOutcome>(Channel.UNLIMITED)
    val outcomes: Flow<NetworkedIdentityOutcome> = outcomeChannel.receiveAsFlow()

    /** Identifies the current attempt. Responses from a cancelled or restarted attempt are ignored. */
    private var attempt = 0
    private var configuration: Deferred<Boolean>? = null
    private var account: NetworkedIdentityLinkAccount? = null
        set(value) {
            field = value
            accountEmail.value = value?.email
        }
    private var attached: VerificationPageData? = null
    private var otpGeneration = 0
    private var task: Job? = null
    private var mutation: Deferred<Result<VerificationPageData>>? = null
    private var previewLookup: Deferred<Result<NetworkedIdentityLinkAccount?>>? = null

    fun startReuse() = start(NetworkedIdentityMode.Reuse)

    fun startSave() = start(NetworkedIdentityMode.Save)

    /**
     * Looks up the provided email's Link account without sending a code or opening the sheet. A handed-in
     * session already names its account, and without an email there's nothing to check.
     */
    fun lookUpProvidedAccountEmail() {
        val email = config.merchantEmail
        val publishableKey = config.merchantPublishableKey
        if (!entry.value.linkAvailable || handoff != null || email.isNullOrBlank() || publishableKey.isNullOrBlank()) {
            return
        }
        if (state.value.isSheetVisible || account != null) return
        val lookup = previewLookup ?: scope.async {
            if (!ensureConfigured(publishableKey)) {
                return@async Result.failure(LinkNotConfigured())
            }
            linkSession.lookup(email)
        }.also { previewLookup = it }
        val id = attempt
        scope.launch {
            val found = lookup.await().getOrNull()
            if (id == attempt) account = found
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
        lookup(normalizedEmail)
    }

    fun submitPhone(phoneNumber: String, country: String) {
        val current = state.value as? NetworkedIdentityState.CollectPhone ?: return
        if (phoneNumber.isBlank() || country.isBlank()) return
        transition(NetworkedIdentityState.SignUpPending(current.email))
        perform(
            operation = {
                linkSession.signUp(email = current.email, phoneNumber = phoneNumber, country = country, name = null)
            },
            onSuccess = ::onAccount,
            onFailure = { error ->
                // Staying on the step keeps the reason visible; dismissing the sheet would hide it.
                transition(
                    NetworkedIdentityState.CollectPhone(
                        email = current.email,
                        error = error.message ?: "Could not create the Link account.",
                    )
                )
            },
        )
    }

    fun submitOtp(code: String) {
        if (state.value !is NetworkedIdentityState.AwaitingOtp ||
            code.length != OTP_LENGTH || code.any { it !in '0'..'9' }
        ) {
            return
        }
        transition(NetworkedIdentityState.OtpConfirmPending(redactedPhoneNumber, otpGeneration))
        perform(
            operation = { linkSession.confirmVerification(code) },
            onSuccess = { verified ->
                account = verified
                proceedAuthenticated()
            },
            onFailure = ::handleConfirmationError,
        )
    }

    fun resendOtp() {
        if (state.value !is NetworkedIdentityState.AwaitingOtp) return
        sendCode(isResend = true)
    }

    fun selectDocument(documentId: String) {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        if (selection.documents.none { it.id == documentId }) return
        transition(selection.copy(selectedDocumentId = documentId))
    }

    fun shareSelectedDocument() {
        val selection = state.value as? NetworkedIdentityState.SelectDocument ?: return
        val document = selection.documents.firstOrNull { it.id == selection.selectedDocumentId } ?: return
        val credentials = account?.credentials
            ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable, error = null)
        transition(NetworkedIdentityState.SharingDocument(document))
        perform(
            operation = {
                // Minted only after the explicit tap and redeemed once: association tokens are single-use.
                repository.createAssociationToken(credentials, document.id).mapCatching { token ->
                    coroutineContext.ensureActive()
                    // #TODO - Networked Identity [NI-Contract]: recovery for ambiguous attach failures. Never
                    // replay a token.
                    mutate(NetworkedIdentityRoute.ResumeReuse) {
                        actions.attachDocument(token.associationToken)
                    }.getOrThrow()
                }
            },
            onSuccess = { data ->
                attached = data
                transition(NetworkedIdentityState.DocumentShared(document))
            },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    fun continueAfterSuccess() {
        val outcome = when (val current = state.value) {
            is NetworkedIdentityState.DocumentShared ->
                NetworkedIdentityOutcome.DocumentShared(current.document, attached ?: return)
            NetworkedIdentityState.SavePrepared -> NetworkedIdentityOutcome.SavePrepared
            else -> return
        }
        attempt += 1
        transition(NetworkedIdentityState.Idle)
        outcomeChannel.trySend(outcome)
    }

    fun chooseManualCapture() {
        if (mode.value != NetworkedIdentityMode.Reuse || state.value == NetworkedIdentityState.SkipPending) return
        invalidateAttempt()
        transition(NetworkedIdentityState.SkipPending)
        perform(
            operation = { mutate(NetworkedIdentityRoute.OrdinaryIdentity) { actions.skip() } },
            onSuccess = { updated ->
                attached = null
                transition(NetworkedIdentityState.Idle)
                outcomeChannel.trySend(NetworkedIdentityOutcome.ManualCapture(updated))
            },
            // Skip only records the choice. If it can't (e.g. the backend doesn't support Networked Identity
            // for this session yet), the user still continues with ordinary verification.
            onFailure = {
                transition(NetworkedIdentityState.Idle)
                outcomeChannel.trySend(NetworkedIdentityOutcome.Fallback(NetworkedIdentityFallbackReason.Unavailable))
            },
        )
    }

    fun cancel() {
        if (!state.value.isSheetVisible) return
        invalidateAttempt()
        transition(NetworkedIdentityState.Cancelled)
        outcomeChannel.trySend(NetworkedIdentityOutcome.Cancelled)
    }

    /** Permanent owner removal: stops all work without reporting an outcome. */
    fun abandon() {
        invalidateAttempt()
        onVerificationUpdate = null
        scope.cancel()
        mutableState.value = NetworkedIdentityState.Cancelled
    }

    private fun start(newMode: NetworkedIdentityMode) {
        if (state.value.isSheetVisible) return
        invalidateAttempt()
        mutableMode.value = newMode
        val publishableKey = config.merchantPublishableKey
        val available = when (newMode) {
            NetworkedIdentityMode.Reuse -> entry.value.reuseAvailable
            NetworkedIdentityMode.Save -> entry.value.offersSave
        }
        if (publishableKey.isNullOrBlank() || !available) {
            return fallBack(NetworkedIdentityFallbackReason.Unavailable, error = null)
        }
        if (newMode == NetworkedIdentityMode.Save && hasPreparedSave.value) {
            return transition(NetworkedIdentityState.SavePrepared)
        }
        transition(NetworkedIdentityState.Preparing)
        perform(
            operation = { Result.success(ensureConfigured(publishableKey)) },
            onSuccess = { ready ->
                if (ready) {
                    continueAfterConfiguration()
                } else {
                    fallBack(NetworkedIdentityFallbackReason.Unavailable, LinkNotConfigured())
                }
            },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    /** Configures Link once; concurrent callers share the same attempt, and a failure allows a retry. */
    private suspend fun ensureConfigured(publishableKey: String): Boolean {
        val pending = configuration
            ?: scope.async { linkSession.configure(publishableKey, merchantDisplayName).isSuccess }
                .also { configuration = it }
        val ready = pending.await()
        if (!ready && configuration === pending) configuration = null
        return ready
    }

    private fun continueAfterConfiguration() {
        if (config.usesLinkUI && handoff == null && account?.isVerified != true) {
            return authenticateWithLinkUI(config.merchantEmail)
        }
        account?.let { return onAccount(it) }
        previewLookup?.let { lookup ->
            return perform(
                operation = { lookup.await() },
                onSuccess = { found ->
                    previewLookup = null
                    onLookup(found, email = null)
                },
                onFailure = {
                    previewLookup = null
                    continueWithEmail(config.merchantEmail)
                },
            )
        }
        val handedIn = handoff ?: return continueWithEmail(config.merchantEmail)
        perform(
            operation = {
                linkSession.restore(
                    NetworkedIdentityCredentials(
                        publishableKey = handedIn.consumerPublishableKey,
                        sessionClientSecret = handedIn.consumerSessionClientSecret,
                    )
                )
            },
            onSuccess = ::onAccount,
            // An expired or invalid handed-in session only loses the shortcut.
            onFailure = { continueWithEmail(handedIn.email) },
        )
    }

    private fun continueWithEmail(knownEmail: String?) {
        when {
            config.usesLinkUI -> authenticateWithLinkUI(knownEmail)
            knownEmail.isNullOrBlank() -> transition(NetworkedIdentityState.CollectEmail)
            else -> lookup(knownEmail)
        }
    }

    private fun lookup(email: String) {
        transition(NetworkedIdentityState.LookupPending)
        perform(
            operation = { linkSession.lookup(email) },
            onSuccess = { found -> onLookup(found, email) },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    private fun onLookup(found: NetworkedIdentityLinkAccount?, email: String?) {
        val signUpEmail = email ?: config.merchantEmail
        when {
            found != null -> onAccount(found)
            mode.value == NetworkedIdentityMode.Save && signUpEmail != null ->
                transition(NetworkedIdentityState.CollectPhone(signUpEmail))
            else -> fallBack(NetworkedIdentityFallbackReason.NoLinkAccount, error = null)
        }
    }

    private fun onAccount(found: NetworkedIdentityLinkAccount) {
        account = found
        when {
            found.isVerified -> proceedAuthenticated()
            config.usesLinkUI -> authenticateWithLinkUI(found.email)
            else -> sendCode(isResend = false)
        }
    }

    /** Hands sign-in, sign-up and code verification to Link's own screens, then continues in Identity's sheet. */
    private fun authenticateWithLinkUI(email: String?) {
        transition(NetworkedIdentityState.LinkAuthentication)
        val mode = mode.value
        perform(
            operation = { linkSession.authenticateWithLinkUI(email, mode) },
            onSuccess = { authenticated ->
                if (authenticated == null) {
                    cancel()
                } else {
                    account = authenticated
                    proceedAuthenticated()
                }
            },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    private fun sendCode(isResend: Boolean) {
        transition(NetworkedIdentityState.OtpStartPending)
        perform(
            operation = { linkSession.startVerification(isResend) },
            onSuccess = { updated ->
                account = updated
                otpGeneration += 1
                transition(awaitingOtp(invalidCode = false))
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

    private fun handleConfirmationError(error: Throwable) {
        when (error.consumerErrorCode) {
            INVALID_CODE -> transition(awaitingOtp(invalidCode = true))
            VERIFICATION_EXPIRED -> sendCode(isResend = false)
            SESSION_EXPIRED -> requireReauthentication()
            else -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error)
        }
    }

    private fun proceedAuthenticated() {
        val credentials = account?.credentials
            ?: return fallBack(NetworkedIdentityFallbackReason.Unavailable, MissingConsumerSession())
        when (mode.value) {
            NetworkedIdentityMode.Reuse -> loadDocuments(credentials)
            NetworkedIdentityMode.Save -> recordSaveConsent(credentials)
        }
    }

    private fun loadDocuments(credentials: NetworkedIdentityCredentials) {
        transition(NetworkedIdentityState.DocumentsPending)
        perform(
            operation = { repository.listDocuments(credentials) },
            onSuccess = { documents ->
                val eligible = documentRequirements.filter(documents, currentTimeSeconds())
                if (eligible.isEmpty()) {
                    fallBack(NetworkedIdentityFallbackReason.NoReusableDocuments, error = null)
                } else {
                    // Sharing always needs an explicit tap; a single document is only preselected.
                    transition(
                        NetworkedIdentityState.SelectDocument(
                            documents = eligible,
                            selectedDocumentId = eligible.singleOrNull()?.id,
                        )
                    )
                }
            },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    private fun recordSaveConsent(credentials: NetworkedIdentityCredentials) {
        transition(NetworkedIdentityState.SavePending)
        perform(
            operation = {
                // #TODO - Networked Identity [NI-Contract]: confirm prepare_document_save is accepted after the
                // verification is submitted; the design hasn't settled whether saving is offered before or after
                // capture.
                repository.createSaveAssociationToken(credentials, actions.verificationSessionId).mapCatching { token ->
                    coroutineContext.ensureActive()
                    mutate(NetworkedIdentityRoute.ResumeSave) {
                        actions.prepareDocumentSave(token.associationToken)
                    }.getOrThrow()
                }
            },
            onSuccess = { transition(NetworkedIdentityState.SavePrepared) },
            onFailure = { error -> fallBack(NetworkedIdentityFallbackReason.Unavailable, error) },
        )
    }

    private fun requireReauthentication() {
        account = null
        previewLookup = null
        transition(NetworkedIdentityState.ReauthenticationRequired)
    }

    private fun fallBack(reason: NetworkedIdentityFallbackReason, error: Throwable?) {
        val current = state.value
        if (current == NetworkedIdentityState.Cancelled ||
            current is NetworkedIdentityState.FullCaptureFallback ||
            current is NetworkedIdentityState.SaveFailed
        ) {
            return
        }
        invalidateAttempt()
        // Saving has no capture to fall back to: keep the sheet open and say what went wrong.
        if (mode.value == NetworkedIdentityMode.Save) {
            return transition(NetworkedIdentityState.SaveFailed(error?.details))
        }
        transition(NetworkedIdentityState.FullCaptureFallback(reason))
        outcomeChannel.trySend(NetworkedIdentityOutcome.Fallback(reason))
    }

    private fun transition(newState: NetworkedIdentityState) {
        mutableState.value = newState
    }

    private fun invalidateAttempt() {
        attempt += 1
        task?.cancel()
        task = null
    }

    /**
     * Commits a Networked Identity action. A request already sent can still commit after its attempt is cancelled,
     * so every later write waits for it, and the committed verification always reaches [onVerificationUpdate].
     */
    private suspend fun mutate(
        newRoute: NetworkedIdentityRoute,
        operation: suspend () -> Result<VerificationPageData>,
    ): Result<VerificationPageData> {
        mutation?.await()
        coroutineContext.ensureActive()
        val pending = scope.async {
            operation().mapCatching { updated ->
                val writable = updated.id == actions.verificationSessionId &&
                    updated.requirements.errors.isEmpty() &&
                    updated.status != VerificationPageData.Status.CANCELED
                if (!writable) throw UnexpectedVerificationResponse()
                // Dismissing the Link UI cannot undo a committed write. Keep the host's requirements current.
                route.value = newRoute
                onVerificationUpdate?.invoke(updated)
                updated
            }
        }
        mutation = pending
        return pending.await()
    }

    /** Runs [operation] and delivers its result only if the attempt that started it is still current. */
    private fun <T> perform(
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val id = attempt
        task = scope.launch {
            val result = try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Result.failure(error)
            }
            if (id != attempt) return@launch
            task = null
            result.fold(onSuccess, onFailure)
        }
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

    private class LinkNotConfigured : IllegalStateException("Link couldn't be configured.")

    private class UnexpectedVerificationResponse :
        IllegalStateException("Networked Identity returned an unexpected verification.")

    private val Throwable.consumerErrorCode: String?
        get() = (this as? StripeException)?.stripeError?.code

    private companion object {
        const val OTP_LENGTH = 6
        const val INVALID_CODE = "consumer_verification_code_invalid"
        const val VERIFICATION_EXPIRED = "consumer_verification_expired"
        const val SESSION_EXPIRED = "consumer_session_expired"
    }
}

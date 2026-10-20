package com.stripe.android.identity.networked

import androidx.annotation.MainThread
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageData
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
 * [startReuse] from the intro, [startSave] from the save screen before submitting, and sharing or continuing
 * always need a tap. Link's own screens sign the user in; a handed-in session key skips them until a call
 * rejects it, and a known email prefills them. All actions and request completions run on the dispatcher
 * supplied by the owner.
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
    /** Receives every verification a Networked Identity action committed, so the host's requirements stay current. */
    private val onVerificationUpdate: (VerificationPageData) -> Boolean,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val mutableState = MutableStateFlow<NetworkedIdentityState>(NetworkedIdentityState.Idle)
    val state: StateFlow<NetworkedIdentityState> = mutableState.asStateFlow()

    private val mutableMode = MutableStateFlow(NetworkedIdentityMode.Reuse)
    val mode: StateFlow<NetworkedIdentityMode> = mutableMode.asStateFlow()

    private val route = MutableStateFlow(config.route)

    /** What the intro and the save screen offer. */
    val entry: StateFlow<NetworkedIdentityEntry> = route.mapAsStateFlow { route ->
        NetworkedIdentityEntry(config = config, handoff = handoff, route = route)
    }

    private val outcomeChannel = Channel<NetworkedIdentityOutcome>(Channel.UNLIMITED)
    val outcomes: Flow<NetworkedIdentityOutcome> = outcomeChannel.receiveAsFlow()

    /** Identifies the current attempt. Responses from a cancelled or restarted attempt are ignored. */
    private var attempt = 0
    private var configuration: Deferred<Boolean>? = null
    private var account: NetworkedIdentityLinkAccount? = null

    /** Set once a consumer call rejects the handed-in key; the user then signs in with Link instead. */
    private var handoffRejected = false

    private var task: Job? = null
    private var mutation: Deferred<Result<VerificationPageData>>? = null

    fun startReuse() = start(NetworkedIdentityMode.Reuse)

    fun startSave() = start(NetworkedIdentityMode.Save)

    /**
     * Records that the user declined saving, which the backend tells apart from closing the offer. The submit that
     * follows doesn't depend on it, so a failure is ignored.
     */
    suspend fun declineSave() {
        mutate(NetworkedIdentityRoute.OrdinaryIdentity) { actions.skip() }
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
            ?: return fallBack(error = null)
        transition(NetworkedIdentityState.SharingDocument(document))
        perform(
            operation = {
                // Minted only after the explicit tap.
                redeem(
                    newRoute = NetworkedIdentityRoute.ResumeReuse,
                    mint = {
                        repository.createAssociationToken(credentials, document.id, actions.verificationSessionId)
                            .map { it.associationToken }
                    },
                    redemption = actions::attachDocument,
                )
            },
            onSuccess = { transition(NetworkedIdentityState.DocumentShared(document)) },
            onFailure = { error -> fallBack(error) },
        )
    }

    fun continueAfterSuccess() {
        val outcome = when (val current = state.value) {
            is NetworkedIdentityState.DocumentShared -> NetworkedIdentityOutcome.DocumentShared
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
            onSuccess = {
                transition(NetworkedIdentityState.Idle)
                outcomeChannel.trySend(NetworkedIdentityOutcome.ManualCapture)
            },
            // Skip only records the choice. If it can't (e.g. the backend doesn't support Networked Identity
            // for this session yet), the user still continues with ordinary verification.
            onFailure = {
                transition(NetworkedIdentityState.Idle)
                outcomeChannel.trySend(NetworkedIdentityOutcome.Fallback)
            },
        )
    }

    fun cancel() {
        if (!state.value.isInProgress) return
        // The save is recorded, so closing its confirmation finishes like Done.
        if (state.value == NetworkedIdentityState.SavePrepared) return continueAfterSuccess()
        // A save being written can still commit after a cancel and leave the save screen offering it again.
        if (state.value == NetworkedIdentityState.SavePending) return
        invalidateAttempt()
        transition(NetworkedIdentityState.Cancelled)
        outcomeChannel.trySend(NetworkedIdentityOutcome.Cancelled)
    }

    /** Permanent owner removal: stops all work without reporting an outcome. */
    fun abandon() {
        invalidateAttempt()
        scope.cancel()
        mutableState.value = NetworkedIdentityState.Cancelled
    }

    private fun start(newMode: NetworkedIdentityMode) {
        if (state.value.isInProgress) return
        invalidateAttempt()
        mutableMode.value = newMode
        // Already recorded, for example before a submit that failed: confirm it instead of saving again.
        if (newMode == NetworkedIdentityMode.Save && route.value == NetworkedIdentityRoute.ResumeSave) {
            return transition(NetworkedIdentityState.SavePrepared)
        }
        val publishableKey = config.merchantPublishableKey
        val available = when (newMode) {
            NetworkedIdentityMode.Reuse -> entry.value.offersReuse
            NetworkedIdentityMode.Save -> entry.value.offersSave
        }
        if (publishableKey.isNullOrBlank() || !available) {
            return fallBack(error = null)
        }
        transition(NetworkedIdentityState.Preparing)
        perform(
            operation = { Result.success(ensureConfigured(publishableKey)) },
            onSuccess = { ready ->
                if (ready) {
                    continueAfterConfiguration()
                } else {
                    fallBack(LinkNotConfigured())
                }
            },
            onFailure = { error -> fallBack(error) },
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
        val current = account
        val handedIn = handoff
        when {
            current != null && (current.isVerified || handedIn != null) -> onAccount(current)
            handedIn == null -> authenticateWithLinkUI(config.merchantEmail)
            handoffRejected -> authenticateWithLinkUI(handedIn.email)
            // The first consumer call checks the key; see [checkHandedInKey].
            else -> onAccount(
                NetworkedIdentityLinkAccount(
                    email = handedIn.email,
                    isVerified = true,
                    credentials = NetworkedIdentityCredentials(handedIn.linkSessionKey),
                )
            )
        }
    }

    private fun onAccount(found: NetworkedIdentityLinkAccount) {
        account = found
        if (found.isVerified) proceedAuthenticated() else authenticateWithLinkUI(found.email)
    }

    /** Hands sign-in, sign-up and code verification to Link's own screens, then continues in Identity's sheet. */
    private fun authenticateWithLinkUI(email: String?) {
        transition(NetworkedIdentityState.LinkAuthentication)
        val mode = mode.value
        perform(
            operation = { linkSession.authenticateWithLinkUI(email, config.phoneNumberAutofillHint, mode) },
            onSuccess = { authenticated ->
                if (authenticated == null) {
                    cancel()
                } else {
                    account = authenticated
                    proceedAuthenticated()
                }
            },
            onFailure = { error -> fallBack(error) },
        )
    }

    private fun proceedAuthenticated() {
        val credentials = account?.credentials
            ?: return fallBack(MissingConsumerSession())
        when (mode.value) {
            NetworkedIdentityMode.Reuse -> loadDocuments(credentials)
            NetworkedIdentityMode.Save -> recordSaveConsent(credentials)
        }
    }

    private fun loadDocuments(credentials: NetworkedIdentityCredentials) {
        transition(NetworkedIdentityState.DocumentsPending)
        perform(
            operation = { repository.listDocuments(credentials).checkHandedInKey(credentials) },
            onSuccess = { documents ->
                val eligible = documentRequirements.filter(documents, currentTimeSeconds())
                if (eligible.isEmpty()) {
                    fallBack(error = null)
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
            onFailure = ::fallBackOrSignIn,
        )
    }

    private fun recordSaveConsent(credentials: NetworkedIdentityCredentials) {
        transition(NetworkedIdentityState.SavePending)
        perform(
            operation = {
                redeem(
                    newRoute = NetworkedIdentityRoute.ResumeSave,
                    mint = {
                        repository.createSaveAssociationToken(credentials, actions.verificationSessionId)
                            .checkHandedInKey(credentials)
                            .map { it.associationToken }
                    },
                    redemption = actions::prepareDocumentSave,
                )
            },
            onSuccess = { transition(NetworkedIdentityState.SavePrepared) },
            onFailure = ::fallBackOrSignIn,
        )
    }

    /**
     * Marks a 401 or 403 for the handed-in key, e.g. expired or not verified yet. Other keys keep their errors, so a
     * rejection after the user's own sign-in falls back instead of asking them to sign in again.
     */
    private fun <T> Result<T>.checkHandedInKey(credentials: NetworkedIdentityCredentials): Result<T> {
        val status = (exceptionOrNull() as? StripeException)?.statusCode
        val isHandedInKey = credentials.linkSessionKey == handoff?.linkSessionKey
        return if (isHandedInKey && status in KEY_REJECTED_STATUS_CODES) Result.failure(HandedInKeyRejected()) else this
    }

    /** A rejected handed-in key only loses the shortcut: the user signs in with Link's screens instead. */
    private fun fallBackOrSignIn(error: Throwable) {
        val handedIn = handoff
        if (error !is HandedInKeyRejected || handedIn == null) return fallBack(error)
        handoffRejected = true
        account = null
        authenticateWithLinkUI(handedIn.email)
    }

    private fun fallBack(error: Throwable?) {
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
        transition(NetworkedIdentityState.FullCaptureFallback)
        outcomeChannel.trySend(NetworkedIdentityOutcome.Fallback)
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
     * so every later write waits for it, and the committed verification always reaches [onVerificationUpdate], which
     * rejects one that doesn't apply to this session.
     */
    private suspend fun mutate(
        newRoute: NetworkedIdentityRoute,
        operation: suspend () -> Result<VerificationPageData>,
    ): Result<VerificationPageData> {
        mutation?.await()
        coroutineContext.ensureActive()
        val pending = scope.async {
            operation().mapCatching { updated ->
                // Dismissing the Link UI cannot undo a committed write. Keep the host's requirements current.
                if (!onVerificationUpdate(updated)) throw UnexpectedVerificationResponse()
                route.value = newRoute
                updated
            }
        }
        mutation = pending
        return pending.await()
    }

    /**
     * Mints a single-use token and redeems it. When the redemption is rejected or its response is lost, the session is
     * read back first, because the write may have committed; otherwise one fresh token is minted and redeemed. If it
     * can't be read back, the redemption fails rather than risk a second write. A token is never replayed.
     */
    private suspend fun redeem(
        newRoute: NetworkedIdentityRoute,
        mint: suspend () -> Result<String>,
        redemption: suspend (String) -> Result<VerificationPageData>,
    ): Result<VerificationPageData> {
        var retriesLeft = 1
        while (true) {
            coroutineContext.ensureActive()
            val token = mint().getOrElse { return Result.failure(it) }
            coroutineContext.ensureActive()
            val result = mutate(newRoute) { redemption(token) }
            val error = result.exceptionOrNull()
            if (error == null || !error.mayRetryRedemption) return result
            actions.verificationIfCommitted(newRoute).getOrElse { return result }?.let { committed ->
                return mutate(newRoute) { Result.success(committed) }
            }
            if (retriesLeft == 0) return result
            retriesLeft -= 1
        }
    }

    /**
     * Whether a failed redemption may be retried with a fresh token: the token was rejected (400) or already used
     * (429), or the response was lost and the write may have committed.
     */
    private val Throwable.mayRetryRedemption: Boolean
        get() = when {
            this is APIConnectionException -> true
            this !is StripeException -> false
            stripeError?.code == NETWORKED_IDENTITY_UNAVAILABLE -> false
            stripeError == null && statusCode in HTTP_SUCCESS -> true
            else -> statusCode in RETRYABLE_STATUS_CODES || statusCode >= HTTP_SERVER_ERROR
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

    private val Throwable.details: String
        get() = (this as? StripeException)?.stripeError?.code?.let { code -> "${message.orEmpty()} ($code)".trim() }
            ?: message
            ?: javaClass.simpleName

    private class MissingConsumerSession : IllegalStateException("Link returned no consumer session.")

    private class HandedInKeyRejected : IllegalStateException("Link rejected the handed-in session key.")

    private class LinkNotConfigured : IllegalStateException("Link couldn't be configured.")

    private class UnexpectedVerificationResponse :
        IllegalStateException("Networked Identity returned an unexpected verification.")

    private companion object {
        const val NETWORKED_IDENTITY_UNAVAILABLE = "networked_identity_unavailable"
        const val HTTP_SERVER_ERROR = 500

        /** A rejected (400) or already used (429) token. */
        val RETRYABLE_STATUS_CODES = setOf(400, 429)
        val KEY_REJECTED_STATUS_CODES = setOf(401, 403)
        val HTTP_SUCCESS = 200..299
    }
}

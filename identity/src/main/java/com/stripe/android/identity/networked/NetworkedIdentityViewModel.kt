package com.stripe.android.identity.networked

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.identity.BuildConfig
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Own this ViewModel in the Identity activity's ViewModelStore, so the intro and the success screen
 * share one attempt. Configuration recreation retains the flow; removing the owner permanently abandons it.
 */
@Suppress("TooManyFunctions")
internal class NetworkedIdentityViewModel(
    private val coordinator: NetworkedIdentityCoordinator,
) : ViewModel() {
    val state = coordinator.state
    val mode = coordinator.mode
    val saved = coordinator.saved
    val outcomes = coordinator.outcomes

    /** What the intro and success screens offer; see [refreshEntry]. */
    val entry = coordinator.entry

    /** Whether a saved ID was shared in this verification. */
    val shared = coordinator.shared

    init {
        refreshEntry()
    }

    /** Checks whether the provided email has a Link account, without starting anything. */
    fun refreshEntry() = coordinator.refreshEntry()

    fun startReuse() = coordinator.startReuse()

    fun startSave() = coordinator.startSave()

    fun submitEmail(email: String) = coordinator.submitEmail(email)

    fun submitPhone(phoneNumber: String, country: String) = coordinator.submitPhone(phoneNumber, country)

    fun submitOtp(code: String) = coordinator.submitOtp(code)

    fun resendOtp() = coordinator.resendOtp()

    fun selectDocument(documentId: String) = coordinator.selectDocument(documentId)

    fun shareSelectedDocument() = coordinator.shareSelectedDocument()

    fun continueAfterSuccess() = coordinator.continueAfterSuccess()

    fun useManualCapture() = coordinator.useManualCapture()

    fun cancel() = coordinator.cancel()

    /** Call for explicit external dismissal, including when the host retains its ViewModelStore. */
    fun abandon() = coordinator.abandon()

    fun screenActions() = NetworkedIdentityScreenActions(
        onSubmitEmail = ::submitEmail,
        onSubmitPhone = ::submitPhone,
        onSubmitOtp = ::submitOtp,
        onResendOtp = ::resendOtp,
        onSelectDocument = ::selectDocument,
        onShareDocument = ::shareSelectedDocument,
        onContinue = ::continueAfterSuccess,
        onManualCapture = ::useManualCapture,
        onCancel = ::cancel,
    )

    override fun onCleared() {
        coordinator.abandon()
        super.onCleared()
    }

    internal class Factory(
        private val application: Application,
        private val verificationPage: VerificationPage,
        private val options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions?,
        private val verificationSessionId: String,
        private val ephemeralKey: String,
        private val workContext: CoroutineContext,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val config = NetworkedIdentityConfig.from(verificationPage, overrides = options?.toDebugOverrides())
            val handoff = options?.linkSessionHandoff
            val backendRepository = DefaultNetworkedIdentityRepository.create(
                merchantRequestOptions = ApiRequest.Options(
                    apiKey = config.merchantPublishableKey.orEmpty(),
                    stripeAccount = null,
                    idempotencyKey = null,
                ),
                workContext = workContext,
            )
            val actions = DefaultNetworkedIdentityActions.create(verificationSessionId, ephemeralKey)
            val coordinator = NetworkedIdentityCoordinator(
                linkSession = LinkControllerNetworkedIdentityLinkSession.create(
                    application = application,
                    savedStateHandle = extras.createSavedStateHandle(),
                ),
                repository = if (config.seedSavedDocuments) {
                    SeededDocumentsNetworkedIdentityRepository(backendRepository, ::nowSeconds)
                } else {
                    backendRepository
                },
                actions = if (config.seedSavedDocuments) SeededDocumentsNetworkedIdentityActions(actions) else actions,
                documentRequirements = NetworkedIdentityDocumentRequirements.fromVerificationPage(verificationPage),
                config = config,
                handoff = handoff,
                merchantDisplayName = application.applicationInfo.loadLabel(application.packageManager).toString(),
                currentTimeSeconds = ::nowSeconds,
                dispatcher = Dispatchers.Main,
            )
            return NetworkedIdentityViewModel(coordinator) as T
        }

        private fun nowSeconds() = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }
}

/**
 * What the Identity screens offer, driven by data rather than a route: a handed-in session or a lookup of
 * the provided email decides whether the intro offers reuse, and the success screen always offers saving.
 */
internal data class NetworkedIdentityEntry(
    /** A merchant publishable key is known, so Link can be offered at all. */
    val linkAvailable: Boolean,
    /** The Link account's email: the handed-in session's, or the provided email once a lookup found it. */
    val accountEmail: String?,
    /** No handed-in session and no provided email: offer Link without a chip and ask for the email. */
    val needsEmail: Boolean,
) {
    /** The intro offers reuse when there's an account to reuse, or no email to check. */
    val offersReuse: Boolean
        get() = linkAvailable && (accountEmail != null || needsEmail)

    internal companion object {
        fun initial(
            config: NetworkedIdentityConfig,
            handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
        ) = NetworkedIdentityEntry(
            linkAvailable = !config.merchantPublishableKey.isNullOrBlank(),
            accountEmail = handoff?.email,
            needsEmail = handoff == null && config.merchantEmail.isNullOrBlank(),
        )
    }
}

/**
 * The activity-scoped Networked Identity ViewModel, or null when Networked Identity isn't set up for
 * this verification or the VerificationPage isn't loaded yet.
 */
@Composable
internal fun rememberNetworkedIdentityViewModel(identityViewModel: IdentityViewModel): NetworkedIdentityViewModel? {
    // Host options stand in for the draft VerificationPage fields until the backend returns them;
    // without options, Networked Identity only runs in builds with the preview flag.
    val options = identityViewModel.verificationArgs.networkedIdentity
    if (options == null && !BuildConfig.NETWORKED_IDENTITY_PREVIEW) return null
    val pageResource by identityViewModel.verificationPage.observeAsState()
    val verificationPage = pageResource?.data ?: return null
    return viewModel(
        key = NETWORKED_IDENTITY_VIEW_MODEL_KEY,
        factory = NetworkedIdentityViewModel.Factory(
            application = identityViewModel.getApplication(),
            verificationPage = verificationPage,
            options = options,
            verificationSessionId = identityViewModel.verificationArgs.verificationSessionId,
            ephemeralKey = identityViewModel.verificationArgs.ephemeralKeySecret,
            workContext = identityViewModel.workContext,
        ),
    )
}

private const val NETWORKED_IDENTITY_VIEW_MODEL_KEY = "NetworkedIdentityViewModel"

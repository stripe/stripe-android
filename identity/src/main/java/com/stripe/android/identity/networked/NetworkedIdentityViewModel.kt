package com.stripe.android.identity.networked

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val outcomes = coordinator.outcomes

    /** What the intro and success screens offer; see [refreshEntry]. */
    val entry = coordinator.entry

    /** Whether this verification's ID is prepared to be saved to Link, so the success screen stops offering it. */
    val hasPreparedSave = coordinator.hasPreparedSave

    private val presentsSheet = MutableStateFlow(true)

    /** Whether Identity's Link sheet is shown: never while Link's own screens authenticate the user. */
    val isSheetVisible: StateFlow<Boolean> = combineAsStateFlow(state, presentsSheet) { state, presentsSheet ->
        presentsSheet && state.presentsIdentitySheet &&
            !(coordinator.usesLinkUI && state == NetworkedIdentityState.Preparing)
    }

    /** Receives every verification a Networked Identity action committed. */
    var onVerificationUpdate: ((VerificationPageData) -> Unit)?
        get() = coordinator.onVerificationUpdate
        set(value) {
            coordinator.onVerificationUpdate = value
        }

    init {
        refreshEntry()
    }

    /** Checks whether the provided email has a Link account, without starting anything. */
    fun refreshEntry() = coordinator.lookUpProvidedAccountEmail()

    fun startReuse() {
        presentsSheet.value = true
        coordinator.startReuse()
    }

    fun startSave() {
        presentsSheet.value = true
        coordinator.startSave()
    }

    /** From the intro: records the choice without showing the sheet. */
    fun chooseManualCaptureFromIntro() {
        presentsSheet.value = false
        coordinator.chooseManualCapture()
    }

    fun submitEmail(email: String) = coordinator.submitEmail(email)

    fun submitPhone(phoneNumber: String, country: String) = coordinator.submitPhone(phoneNumber, country)

    fun submitOtp(code: String) = coordinator.submitOtp(code)

    fun resendOtp() = coordinator.resendOtp()

    fun selectDocument(documentId: String) = coordinator.selectDocument(documentId)

    fun shareSelectedDocument() = coordinator.shareSelectedDocument()

    fun continueAfterSuccess() = coordinator.continueAfterSuccess()

    fun chooseManualCapture() = coordinator.chooseManualCapture()

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
        onManualCapture = ::chooseManualCapture,
        onCancel = ::cancel,
    )

    override fun onCleared() {
        coordinator.abandon()
        super.onCleared()
    }

    internal class Factory(
        private val application: Application,
        private val linkHost: NetworkedIdentityLinkHost,
        private val identityRepository: IdentityRepository,
        private val verificationPage: VerificationPage,
        private val config: NetworkedIdentityConfig,
        private val handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
        private val verificationSessionId: String,
        private val ephemeralKey: String,
        private val workContext: CoroutineContext,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
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
                linkSession = LinkControllerNetworkedIdentityLinkSession(application, linkHost),
                repository = if (config.seedSavedDocuments) {
                    SeededDocumentsNetworkedIdentityRepository(backendRepository, ::nowSeconds)
                } else {
                    backendRepository
                },
                // The seeded Identity repository and these actions share the simulated session.
                actions = (identityRepository as? SeededDocumentsIdentityRepository)?.actions(actions) ?: actions,
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
 * The activity-scoped Networked Identity ViewModel, or null when Networked Identity isn't set up for
 * this verification or the VerificationPage isn't loaded yet.
 */
@Composable
internal fun rememberNetworkedIdentityViewModel(identityViewModel: IdentityViewModel): NetworkedIdentityViewModel? {
    // Host options stand in for the draft VerificationPage fields until the backend returns them;
    // without options, Networked Identity only runs in builds with the preview flag.
    val options = identityViewModel.verificationArgs.networkedIdentity
    if (!NetworkedIdentityLinkHost.isEnabled(options)) return null
    val pageResource by identityViewModel.verificationPage.observeAsState()
    val verificationPage = pageResource?.data ?: return null
    val config = remember(verificationPage, options) {
        NetworkedIdentityConfig.from(verificationPage, overrides = options?.toDebugOverrides())
    }
    if (config.route == NetworkedIdentityRoute.OrdinaryIdentity || config.merchantPublishableKey.isNullOrBlank()) {
        return null
    }
    val linkHost: NetworkedIdentityLinkHost = viewModel(
        key = NetworkedIdentityLinkHost.KEY,
        factory = NetworkedIdentityLinkHost.Factory,
    )
    return viewModel(
        key = NETWORKED_IDENTITY_VIEW_MODEL_KEY,
        factory = NetworkedIdentityViewModel.Factory(
            application = identityViewModel.getApplication(),
            linkHost = linkHost,
            identityRepository = identityViewModel.identityRepository,
            verificationPage = verificationPage,
            config = config,
            handoff = options?.linkSessionHandoff,
            verificationSessionId = identityViewModel.verificationArgs.verificationSessionId,
            ephemeralKey = identityViewModel.verificationArgs.ephemeralKeySecret,
            workContext = identityViewModel.workContext,
        ),
    )
}

private const val NETWORKED_IDENTITY_VIEW_MODEL_KEY = "NetworkedIdentityViewModel"

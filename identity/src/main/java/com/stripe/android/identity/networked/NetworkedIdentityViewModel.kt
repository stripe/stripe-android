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
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.IdentityRepository
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
internal class NetworkedIdentityViewModel(
    private val coordinator: NetworkedIdentityCoordinator,
) : ViewModel() {
    val state = coordinator.state
    val mode = coordinator.mode
    val outcomes = coordinator.outcomes

    /** What the intro and success screens offer. */
    val entry = coordinator.entry

    private val presentsSheet = MutableStateFlow(true)

    /** Whether Identity's Link sheet is shown: never while Link's own screens authenticate the user. */
    val isSheetVisible: StateFlow<Boolean> = combineAsStateFlow(state, presentsSheet) { state, presentsSheet ->
        presentsSheet && state.presentsIdentitySheet
    }

    init {
        coordinator.lookUpProvidedAccountEmail()
    }

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

    fun cancel() = coordinator.cancel()

    fun screenActions() = NetworkedIdentityScreenActions(
        onSelectDocument = coordinator::selectDocument,
        onShareDocument = coordinator::shareSelectedDocument,
        onContinue = coordinator::continueAfterSuccess,
        onManualCapture = coordinator::chooseManualCapture,
        onCancel = coordinator::cancel,
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
        private val onVerificationUpdate: (VerificationPageData) -> Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val actions = DefaultNetworkedIdentityActions.create(
                verificationSessionId = verificationSessionId,
                ephemeralKey = ephemeralKey,
                identityRepository = identityRepository,
                workContext = workContext,
            )
            val coordinator = NetworkedIdentityCoordinator(
                linkSession = LinkControllerNetworkedIdentityLinkSession(application, linkHost),
                repository = if (config.seedSavedDocuments) {
                    SeededDocumentsNetworkedIdentityRepository(::nowSeconds)
                } else {
                    DefaultNetworkedIdentityRepository.create(workContext)
                },
                // The seeded Identity repository and these actions share the simulated session.
                actions = (identityRepository as? SeededDocumentsIdentityRepository)?.actions(actions) ?: actions,
                documentRequirements = NetworkedIdentityDocumentRequirements.fromVerificationPage(verificationPage),
                config = config,
                handoff = handoff,
                merchantDisplayName = application.applicationInfo.loadLabel(application.packageManager).toString(),
                currentTimeSeconds = ::nowSeconds,
                onVerificationUpdate = onVerificationUpdate,
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
    val options = identityViewModel.verificationArgs.networkedIdentity
    val pageResource by identityViewModel.verificationPage.observeAsState()
    val verificationPage = pageResource?.data ?: return null
    val config = remember(verificationPage, options) {
        verificationPage.networkedIdentityConfig(options)
    } ?: return null
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
            onVerificationUpdate = identityViewModel::recordNetworkedIdentityUpdate,
        ),
    )
}

private const val NETWORKED_IDENTITY_VIEW_MODEL_KEY = "NetworkedIdentityViewModel"

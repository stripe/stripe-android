package com.stripe.android.financialconnections.features.genericerror

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.GenericErrorClickPrimaryCta
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.GenericErrorPane
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_AUTO_LAUNCH_AUTH_SESSION
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Backs the [Pane.GENERIC_ERROR] pane: an error screen whose entire contents are dictated by the
 * server through the `extra_fields` of an API error.
 */
internal class GenericErrorViewModel @AssistedInject constructor(
    @Assisted initialState: GenericErrorState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val contentRepository: GenericErrorContentRepository,
    private val getOrFetchSync: GetOrFetchSync,
    private val cancelAuthorizationSession: CancelAuthorizationSession,
    private val updateLocalManifest: UpdateLocalManifest,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
) : FinancialConnectionsViewModel<GenericErrorState>(initialState, nativeAuthFlowCoordinator) {

    init {
        val pane = contentRepository.get()?.pane
        setState { copy(pane = pane) }

        viewModelScope.launch {
            if (pane == null) {
                // There's nothing to render. Rather than strand the user on a blank pane, send
                // them somewhere they can make progress.
                eventTracker.logError(
                    extraMessage = "No generic error pane content to render.",
                    error = UnclassifiedError("GenericErrorPaneMissingContent"),
                    logger = logger,
                    pane = PANE,
                )
                selectAnotherBank()
                return@launch
            }

            eventTracker.track(PaneLoaded(PANE))

            if (pane.primaryCtaAction == null) {
                // We rendered a screen whose button we can't fully honor. Surface it so we notice
                // the server shipping an action this version of the SDK doesn't know about.
                eventTracker.logError(
                    extraMessage = "Unhandled generic error pane primary CTA action.",
                    error = UnclassifiedError("GenericErrorPaneUnknownCtaAction"),
                    logger = logger,
                    pane = PANE,
                )
            }
        }

        onAsync(
            GenericErrorState::primaryCtaClick,
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error handling generic error pane primary CTA click.",
                    error = it,
                    logger = logger,
                    pane = PANE,
                )
            },
        )
    }

    override fun updateTopAppBar(state: GenericErrorState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            // This pane replaces the one the error came from, so there's nothing to go back to.
            // The user moves forward through the primary CTA instead.
            allowBackNavigation = false,
            error = null,
        )
    }

    /**
     * Tracked as [GenericErrorState.primaryCtaClick] so the screen can disable the button while
     * this is in flight, since [restartAuthFlow] makes a network call before navigating away.
     */
    fun onPrimaryCtaClick(): Job = suspend {
        val action = stateFlow.value.pane?.primaryCtaAction
        eventTracker.track(GenericErrorClickPrimaryCta(pane = PANE, action = action?.value))

        cancelPendingAuthSession()

        when (action) {
            PrimaryCtaAction.RestartAuthFlow -> restartAuthFlow()
            null -> selectAnotherBank()
        }
    }.execute { copy(primaryCtaClick = it) }

    fun onClickableTextClick(uri: String) {
        // No clickable links are expected in server-driven error copy.
        logger.debug("Unexpected clickable text in $PANE: $uri")
    }

    /**
     * Cancels the auth session that got us here, since the user is leaving it behind either way.
     */
    private suspend fun cancelPendingAuthSession() {
        runCatching {
            val authSession = getOrFetchSync().manifest.activeAuthSession ?: return@runCatching
            cancelAuthorizationSession(authSession.id)
        }.onFailure {
            // Best effort: failing to cancel shouldn't block the user from retrying.
            logger.error("Failed to cancel the auth session on $PANE", it)
        }
    }

    private suspend fun restartAuthFlow() {
        val institution = getOrFetchSync().manifest.activeInstitution
        if (institution == null) {
            // There's no institution to re-authenticate with, so the best we can do is let the
            // user pick one.
            selectAnotherBank()
            return
        }

        // Cancelling leaves the cancelled session on the cached manifest, and Partner Auth reuses
        // an existing active session rather than creating one. Clear it so we get a fresh session.
        updateLocalManifest { it.copy(activeAuthSession = null) }

        navigationManager.tryNavigateTo(
            route = Destination.PartnerAuth(
                referrer = PANE,
                // The user has already been told what to do, so skip the prepane and hand them
                // straight to the institution.
                extraArgs = mapOf(KEY_AUTO_LAUNCH_AUTH_SESSION to true.toString()),
            ),
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
    }

    private fun selectAnotherBank() {
        navigationManager.tryNavigateTo(
            route = Destination.InstitutionPicker(referrer = PANE),
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
    }

    override fun onCleared() {
        contentRepository.clear()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: GenericErrorState): GenericErrorViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.genericErrorViewModelFactory.create(GenericErrorState())
                }
            }

        internal val PANE = Pane.GENERIC_ERROR
    }
}

internal data class GenericErrorState(
    val pane: GenericErrorPane? = null,
    val primaryCtaClick: Async<Unit> = Async.Uninitialized,
)

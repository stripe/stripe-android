package com.stripe.android.financialconnections.features.consent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentNoticeSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.experimentAssignment
import com.stripe.android.financialconnections.utils.trackExposure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.util.Date

internal class ConsentViewModel @AssistedInject constructor(
    @Assisted initialState: ConsentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val logger: Logger,
    private val presentNoticeSheet: PresentNoticeSheet,
) : FinancialConnectionsViewModel<ConsentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val sync = getOrFetchSync()
            val manifest = sync.manifest
            val shouldShowMerchantLogos: Boolean = manifest
                .experimentAssignment(CONNECTIONS_CONSENT_COMBINED_LOGO) == "treatment"
            eventTracker.trackExposure(CONNECTIONS_CONSENT_COMBINED_LOGO, manifest)
            ConsentState.Payload(
                consent = sync.text!!.consent!!,
                shouldShowMerchantLogos = shouldShowMerchantLogos,
                merchantLogos = sync.visual.merchantLogos
            )
        }.execute { copy(consent = it) }
    }

    override fun updateTopAppBar(state: ConsentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.CONSENT,
            hideStripeLogo = state.consent()?.shouldShowMerchantLogos ?: true,
            allowBackNavigation = true,
            error = state.consent.error,
        )
    }

    private fun logErrors() {
        onAsync(
            ConsentState::consent,
            onSuccess = { eventTracker.track(PaneLoaded(Pane.CONSENT)) },
            onFail = { logger.error("Error retrieving consent content", it) }
        )
        onAsync(ConsentState::acceptConsent, onFail = {
            eventTracker.logError(
                extraMessage = "Error accepting consent",
                error = it,
                logger = logger,
                pane = Pane.CONSENT
            )
        })
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(ConsentAgree)
            FinancialConnections.emitEvent(Name.CONSENT_ACQUIRED)
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            navigationManager.tryNavigateTo(updatedManifest.nextPane.destination(referrer = Pane.CONSENT))
            updatedManifest
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        val date = Date()
        handleClickableUrl(
            currentPane = Pane.CONSENT,
            uri = uri,
            onNetworkUrlClicked = { setState { copy(viewEffect = OpenUrl(uri, date.time)) } },
            knownDeeplinkActions = mapOf(
                // Clicked on the "Data Access" link -> Open the Data Access bottom sheet
                ConsentClickableText.DATA.value to {
                    presentDataAccessBottomSheet()
                },
                // Clicked on the "Legal details" link -> Open the Legal Details bottom sheet
                ConsentClickableText.LEGAL_DETAILS.value to {
                    presentLegalDetailsBottomSheet()
                },
                // Clicked on the "Manual entry" link -> Navigate to the Manual Entry screen
                ConsentClickableText.MANUAL_ENTRY.value to {
                    navigationManager.tryNavigateTo(Destination.ManualEntry(referrer = Pane.CONSENT))
                },
            )
        )
    }

    private fun presentDataAccessBottomSheet() {
        val dataAccessNotice = stateFlow.value.consent()?.consent?.dataAccessNotice ?: return
        presentNoticeSheet(
            content = DataAccess(dataAccessNotice),
            referrer = Pane.CONSENT,
        )
    }

    private fun presentLegalDetailsBottomSheet() {
        val notice = stateFlow.value.consent()?.consent?.legalDetailsNotice ?: return
        presentNoticeSheet(
            content = Legal(notice),
            referrer = Pane.CONSENT,
        )
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ConsentState): ConsentViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.consentViewModelFactory.create(ConsentState())
                }
            }
    }
}

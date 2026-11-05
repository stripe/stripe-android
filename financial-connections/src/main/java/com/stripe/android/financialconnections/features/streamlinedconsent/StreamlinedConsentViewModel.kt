package com.stripe.android.financialconnections.features.streamlinedconsent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.TopAppBarLogoClicked
import com.stripe.android.financialconnections.features.consent.ConsentClickableText
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Generic
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.streamlinedconsent.StreamlinedConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_NEXT_PANE_ON_DISABLE_NETWORKING
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

internal class StreamlinedConsentViewModel @AssistedInject constructor(
    @Assisted initialState: StreamlinedConsentState,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val presentSheet: PresentSheet,
    private val logger: Logger,
) : FinancialConnectionsViewModel<StreamlinedConsentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        suspend {
            val sync = getOrFetchSync()
            val streamlinedConsentPane = requireNotNull(sync.text?.streamlinedConsentPane) {
                "Streamlined consent pane is missing"
            }
            StreamlinedConsentState.Payload(
                streamlinedConsentPane = streamlinedConsentPane,
                // The backend may omit the notice from the streamlined consent pane,
                // in which case we fall back to the one from the regular consent pane.
                dataAccessNotice = streamlinedConsentPane.dataAccessNotice
                    ?: sync.text?.consent?.dataAccessNotice,
            )
        }.execute { copy(payload = it) }

        viewModelScope.launch {
            nativeAuthFlowCoordinator()
                .filterIsInstance<TopAppBarLogoClicked>()
                .collect { message ->
                    if (message.pane == PANE) {
                        onLogoClick()
                    }
                }
        }
    }

    override fun updateTopAppBar(state: StreamlinedConsentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            hideStripeLogo = false,
            // The institution picker remains on the back stack, so users can choose another institution.
            allowBackNavigation = true,
            error = state.payload.error,
            canCloseWithoutConfirmation = true,
            logoClickable = state.payload()?.streamlinedConsentPane?.moreInfoNotice != null,
        )
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(Click(eventName = "click.agree", pane = PANE))
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            eventTracker.emitEvent(Name.CONSENT_ACQUIRED)
            navigationManager.tryNavigateTo(updatedManifest.nextPane.destination(referrer = PANE))
            updatedManifest
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(uri: String) {
        viewModelScope.launch {
            handleClickableUrl(
                currentPane = PANE,
                uri = uri,
                onNetworkUrlClicked = { setState { copy(viewEffect = OpenUrl(uri)) } },
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
                        navigationManager.tryNavigateTo(Destination.ManualEntry(referrer = PANE))
                    },
                    // Clicked on the "Manual entry" link on NME flows -> Navigate to the Link Login Warmup screen
                    ConsentClickableText.LINK_LOGIN_WARMUP.value to {
                        navigationManager.tryNavigateTo(
                            Destination.NetworkingLinkLoginWarmup(
                                referrer = PANE,
                                extraArgs = mapOf(
                                    KEY_NEXT_PANE_ON_DISABLE_NETWORKING to it.nextPaneOrDrawerOnSecondaryCta
                                ),
                            )
                        )
                    },
                    // Surfaces where user has signed in to Link and then launches the auth flow.
                    ConsentClickableText.LINK_ACCOUNT_PICKER.value to {
                        navigationManager.tryNavigateTo(Destination.LinkAccountPicker(referrer = PANE))
                    },
                )
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    private fun onLogoClick() {
        withPayload {
            val moreInfoNotice = it.streamlinedConsentPane.moreInfoNotice ?: return@withPayload
            eventTracker.track(Click(eventName = "click.nav_bar.logo", pane = PANE))
            presentSheet(
                content = Generic(moreInfoNotice),
                referrer = PANE,
            )
        }
    }

    private fun presentDataAccessBottomSheet() {
        withPayload {
            val dataAccessNotice = it.dataAccessNotice
            if (dataAccessNotice == null) {
                eventTracker.logError(
                    extraMessage = "Data access notice is missing",
                    error = IllegalStateException("Data access notice is missing"),
                    logger = logger,
                    pane = PANE,
                )
                return@withPayload
            }
            presentSheet(
                content = DataAccess(dataAccessNotice),
                referrer = PANE,
            )
        }
    }

    private fun presentLegalDetailsBottomSheet() {
        withPayload {
            presentSheet(
                content = Legal(it.streamlinedConsentPane.legalDetailsNotice),
                referrer = PANE,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: StreamlinedConsentState): StreamlinedConsentViewModel
    }

    companion object {

        internal val PANE = Pane.STREAMLINED_CONSENT

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.streamlinedConsentViewModelFactory.create(StreamlinedConsentState())
                }
            }
    }
}

private fun StreamlinedConsentViewModel.withPayload(
    block: (StreamlinedConsentState.Payload) -> Unit,
) {
    val payload = stateFlow.value.payload() ?: return
    block(payload)
}

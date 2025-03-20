package com.stripe.android.financialconnections.features.streamlinedconsent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.consent.ConsentClickableText
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.streamlinedconsent.IDConsentContentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class IDConsentContentViewModel @AssistedInject constructor(
    @Assisted initialState: IDConsentContentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val presentSheet: PresentSheet,
) : FinancialConnectionsViewModel<IDConsentContentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        suspend {
            val sync = getOrFetchSync()
            IDConsentContentState.Payload(
                idConsentContentPane = sync.text!!.idConsentContentPane!!,
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: IDConsentContentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            hideStripeLogo = false,
            allowBackNavigation = false,
            error = state.payload.error,
        )
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(ConsentAgree)
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            FinancialConnections.emitEvent(Name.CONSENT_ACQUIRED)
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
                    // Clicked on the "Legal details" link -> Open the Legal Details bottom sheet
                    ConsentClickableText.LEGAL_DETAILS.value to {
                        presentLegalDetailsBottomSheet()
                    },
                )
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    private fun presentLegalDetailsBottomSheet() {
        withPayload {
            val notice = it.idConsentContentPane.legalDetailsNotice
            presentSheet(
                content = Legal(notice),
                referrer = PANE,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: IDConsentContentState): IDConsentContentViewModel
    }

    companion object {

        private val PANE = FinancialConnectionsSessionManifest.Pane.ID_CONSENT_CONTENT

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.idConsentContentViewModelFactory.create(IDConsentContentState())
                }
            }
    }
}

private fun IDConsentContentViewModel.withPayload(
    block: (IDConsentContentState.Payload) -> Unit,
) {
    val payload = stateFlow.value.payload() ?: return
    block(payload)
}

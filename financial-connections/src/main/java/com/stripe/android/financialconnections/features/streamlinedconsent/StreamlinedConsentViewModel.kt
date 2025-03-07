package com.stripe.android.financialconnections.features.streamlinedconsent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.consent.ConsentClickableText
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Generic
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.streamlinedconsent.StreamlinedConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.STREAMLINED_CONSENT
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_NEXT_PANE_ON_DISABLE_NETWORKING
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkLoginWarmup
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarLogoState
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.util.Date

internal class StreamlinedConsentViewModel @AssistedInject constructor(
    @Assisted initialState: StreamlinedConsentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val presentSheet: PresentSheet,
) : FinancialConnectionsViewModel<StreamlinedConsentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        suspend {
            val sync = getOrFetchSync()
            StreamlinedConsentState.Payload(
                streamlinedConsent = sync.text!!.streamlinedConsent!!,
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: StreamlinedConsentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = STREAMLINED_CONSENT,
            hideStripeLogo = false,
            allowBackNavigation = true,
            error = state.payload.error,
            logoState = TopAppBarLogoState(
                trailingIcon = R.drawable.stripe_ic_lock,
                tint = true,
                onClick = ::presentMoreInfoBottomSheet,
            ),
        )
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(ConsentAgree)
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            FinancialConnections.emitEvent(Name.CONSENT_ACQUIRED)
            navigationManager.tryNavigateTo(updatedManifest.nextPane.destination(referrer = STREAMLINED_CONSENT))
            updatedManifest
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(uri: String) {
        viewModelScope.launch {
            val date = Date()
            handleClickableUrl(
                currentPane = STREAMLINED_CONSENT,
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
                        navigationManager.tryNavigateTo(ManualEntry(referrer = STREAMLINED_CONSENT))
                    },
                    // Clicked on the "Manual entry" link on NME flows -> Navigate to the Link Login Warmup screen
                    ConsentClickableText.LINK_LOGIN_WARMUP.value to {
                        navigationManager.tryNavigateTo(
                            NetworkingLinkLoginWarmup(
                                referrer = STREAMLINED_CONSENT,
                                extraArgs = mapOf(KEY_NEXT_PANE_ON_DISABLE_NETWORKING to it.nextPaneOrDrawerOnSecondaryCta)
                            )
                        )
                    },
                    // Surfaces where user has signed in to Link and then launches the auth flow.
                    ConsentClickableText.LINK_ACCOUNT_PICKER.value to {
                        navigationManager.tryNavigateTo(
                            route = Destination.LinkAccountPicker(referrer = STREAMLINED_CONSENT)
                        )
                    }
                )
            )
        }
    }

    private fun presentDataAccessBottomSheet() {
        withPayload {
            val dataAccessNotice = it.streamlinedConsent.dataAccessNotice ?: return@withPayload
            presentSheet(
                content = DataAccess(dataAccessNotice),
                referrer = STREAMLINED_CONSENT,
            )
        }
    }

    private fun presentLegalDetailsBottomSheet() {
        withPayload {
            val notice = it.streamlinedConsent.legalDetailsNotice
            presentSheet(
                content = Legal(notice),
                referrer = STREAMLINED_CONSENT,
            )
        }
    }

    fun presentMoreInfoBottomSheet() {
        withPayload {
            val moreInfo = it.streamlinedConsent.moreInfoNotice
            presentSheet(
                content = Generic(moreInfo),
                referrer = STREAMLINED_CONSENT,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: StreamlinedConsentState): StreamlinedConsentViewModel
    }

    companion object {

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

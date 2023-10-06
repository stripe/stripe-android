package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.features.consent.ConsentClickableText.DATA
import com.stripe.android.financialconnections.features.consent.ConsentClickableText.LEGAL_DETAILS
import com.stripe.android.financialconnections.features.consent.ConsentClickableText.MANUAL_ENTRY
import com.stripe.android.financialconnections.features.consent.ConsentState.BottomSheetContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ClickHandler
import com.stripe.android.financialconnections.utils.Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO
import com.stripe.android.financialconnections.utils.experimentAssignment
import com.stripe.android.financialconnections.utils.trackExposure
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val clickHandler: ClickHandler,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

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

    private fun logErrors() {
        onAsync(
            ConsentState::consent,
            onSuccess = { eventTracker.track(FinancialConnectionsEvent.PaneLoaded(Pane.CONSENT)) },
            onFail = { logger.error("Error retrieving consent content", it) }
        )
        onAsync(ConsentState::acceptConsent, onFail = {
            eventTracker.track(Error(Pane.CONSENT, it))
            logger.error("Error accepting consent", it)
        })
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(ConsentAgree)
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            navigationManager.tryNavigateTo(updatedManifest.nextPane.destination(referrer = Pane.CONSENT))
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        val date = Date()
        clickHandler.handle(
            uri = uri,
            pane = PANE,
            onNetworkUrlClick = { setState { copy(viewEffect = OpenUrl(uri, date.time)) } },
            clickActions = mapOf(
                LEGAL_DETAILS.value to {
                    setState {
                        copy(
                            currentBottomSheet = BottomSheetContent.LEGAL,
                            viewEffect = OpenBottomSheet(date.time)
                        )
                    }
                },
                DATA.value to {
                    setState {
                        copy(
                            currentBottomSheet = BottomSheetContent.DATA,
                            viewEffect = OpenBottomSheet(date.time)
                        )
                    }
                },
                MANUAL_ENTRY.value to {
                    navigationManager.tryNavigateTo(
                        ManualEntry(referrer = PANE)
                    )
                }
            )
        )
    }


    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    companion object : MavericksViewModelFactory<ConsentViewModel, ConsentState> {

        private val PANE = Pane.CONSENT

        override fun create(
            viewModelContext: ViewModelContext,
            state: ConsentState
        ): ConsentViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .consentBuilder
                .initialState(state)
                .build()
                .viewModel
        }

    }

}

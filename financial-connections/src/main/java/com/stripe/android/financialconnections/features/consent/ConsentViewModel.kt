package com.stripe.android.financialconnections.features.consent

import android.webkit.URLUtil
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.features.consent.ConsentState.BottomSheetContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.financialconnections.utils.experimentAssignment
import com.stripe.android.financialconnections.utils.trackExposure
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val goNext: GoNext,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val uriUtils: UriUtils,
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
                merchantLogos = sync.visual?.merchantLogos ?: emptyList()
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
            goNext(updatedManifest.nextPane)
            Unit
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(uri: String) {
        // if clicked uri contains an eventName query param, track click event.
        viewModelScope.launch {
            uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
                eventTracker.track(Click(eventName, pane = Pane.CONSENT))
            }
        }
        val date = Date()
        if (URLUtil.isNetworkUrl(uri)) {
            setState { copy(viewEffect = OpenUrl(uri, date.time)) }
        } else {
            val managedUri = ConsentClickableText.values()
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                ConsentClickableText.DATA -> {
                    setState {
                        copy(
                            currentBottomSheet = BottomSheetContent.DATA,
                            viewEffect = ViewEffect.OpenBottomSheet(date.time)
                        )
                    }
                }

                ConsentClickableText.MANUAL_ENTRY -> {
                    navigationManager.navigate(NavigationDirections.manualEntry)
                }

                ConsentClickableText.LEGAL_DETAILS -> {
                    setState {
                        copy(
                            currentBottomSheet = BottomSheetContent.LEGAL,
                            viewEffect = ViewEffect.OpenBottomSheet(date.time)
                        )
                    }
                }

                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    companion object : MavericksViewModelFactory<ConsentViewModel, ConsentState> {

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

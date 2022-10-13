package com.stripe.android.financialconnections.features.consent

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
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val goNext: GoNext,
    private val getManifest: GetManifest,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val uriUtils: UriUtils,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    init {
        logErrors()
        viewModelScope.launch {
            val manifest = getManifest()
            setState {
                copy(
                    manualEntryEnabled = manifest.allowManualEntry,
                    manualEntryShowBusinessDaysNotice =
                    !manifest.customManualEntryHandling && manifest.manualEntryUsesMicrodeposits,
                    disconnectUrl = FinancialConnectionsUrlResolver.getDisconnectUrl(manifest),
                    faqUrl = FinancialConnectionsUrlResolver.getFAQUrl(manifest),
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest),
                    stripeToSUrl = FinancialConnectionsUrlResolver.getStripeTOSUrl(manifest),
                    privacyCenterUrl = FinancialConnectionsUrlResolver.getPrivacyCenterUrl(manifest),
                    title = ConsentTextBuilder.getConsentTitle(manifest),
                    bullets = ConsentTextBuilder.getBullets(manifest),
                    requestedDataTitle = ConsentTextBuilder.getDataRequestedTitle(manifest),
                    requestedDataBullets = ConsentTextBuilder.getRequestedDataBullets(manifest)
                )
            }
            // TODO move to payload onSuccess once dynamic consent is in place.
            // TODO log error on payload onError
            eventTracker.track(FinancialConnectionsEvent.PaneLoaded(NextPane.CONSENT))
        }
    }

    private fun logErrors() {
        onAsync(ConsentState::acceptConsent, onFail = {
            eventTracker.track(Error(it))
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

    fun onClickableTextClick(tag: String) {
        val logClick: (String) -> Unit = {
            viewModelScope.launch {
                eventTracker.track(Click(it, pane = NextPane.CONSENT))
            }
        }
        uriUtils.getQueryParameter(tag, "eventName")?.let { logClick(it) }
        when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
            ConsentClickableText.TERMS ->
                setState { copy(viewEffect = OpenUrl(stripeToSUrl)) }

            ConsentClickableText.PRIVACY ->
                setState { copy(viewEffect = OpenUrl(FinancialConnectionsUrls.StripePrivacyPolicy)) }

            ConsentClickableText.DISCONNECT ->
                setState { copy(viewEffect = OpenUrl(disconnectUrl)) }

            ConsentClickableText.DATA -> {
                logClick("click.data_requested")
                setState { copy(viewEffect = ConsentState.ViewEffect.OpenBottomSheet) }
            }

            ConsentClickableText.PRIVACY_CENTER ->
                setState { copy(viewEffect = OpenUrl(privacyCenterUrl)) }

            ConsentClickableText.DATA_ACCESS ->
                setState { copy(viewEffect = OpenUrl(dataPolicyUrl)) }

            ConsentClickableText.MANUAL_ENTRY -> {
                logClick("click.manual_entry")
                navigationManager.navigate(NavigationDirections.manualEntry)
            }

            null -> logger.error("Unrecognized clickable text: $tag")
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

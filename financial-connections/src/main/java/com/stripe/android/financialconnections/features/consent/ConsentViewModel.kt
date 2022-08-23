package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val getManifest: GetManifest,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    init {
        viewModelScope.launch {
            val manifest = getManifest()
            setState {
                copy(
                    manualEntryEnabled = manifest.allowManualEntry,
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
        }
    }

    fun onContinueClick() {
        suspend {
            acceptConsent()
            with(nativeAuthFlowCoordinator()) {
                emit(RequestNextStep(currentStep = NavigationDirections.consent))
            }
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(tag: String) {
        when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
            ConsentClickableText.TERMS ->
                setState { copy(viewEffect = OpenUrl(stripeToSUrl)) }
            ConsentClickableText.PRIVACY ->
                setState { copy(viewEffect = OpenUrl(FinancialConnectionsUrls.StripePrivacyPolicy)) }
            ConsentClickableText.DISCONNECT ->
                setState { copy(viewEffect = OpenUrl(disconnectUrl)) }
            ConsentClickableText.DATA ->
                setState { copy(viewEffect = ConsentState.ViewEffect.OpenBottomSheet) }
            ConsentClickableText.PRIVACY_CENTER ->
                setState { copy(viewEffect = OpenUrl(privacyCenterUrl)) }
            ConsentClickableText.DATA_ACCESS ->
                setState { copy(viewEffect = OpenUrl(dataPolicyUrl)) }
            ConsentClickableText.MANUAL_ENTRY ->
                navigationManager.navigate(NavigationDirections.manualEntry)
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

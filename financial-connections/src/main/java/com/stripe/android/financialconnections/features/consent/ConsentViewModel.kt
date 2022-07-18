package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val repository: FinancialConnectionsManifestRepository,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    init {
        viewModelScope.launch {
            val manifest = repository.getOrFetchManifest()
            setState {
                copy(
                    disconnectUrl = ConsentUrlBuilder.getDisconnectUrl(manifest),
                    faqUrl = ConsentUrlBuilder.getFAQUrl(manifest),
                    dataPolicyUrl = ConsentUrlBuilder.getDataPolicyUrl(manifest),
                    stripeToSUrl = ConsentUrlBuilder.getStripeTOSUrl(manifest),
                    privacyCenterUrl = ConsentUrlBuilder.getPrivacyCenterUrl(manifest),
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
        setState {
            when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
                ConsentClickableText.TERMS ->
                    copy(viewEffect = OpenUrl(stripeToSUrl))
                ConsentClickableText.PRIVACY ->
                    copy(viewEffect = OpenUrl(FinancialConnectionsUrls.StripePrivacyPolicy))
                ConsentClickableText.DISCONNECT ->
                    copy(viewEffect = OpenUrl(disconnectUrl))
                ConsentClickableText.DATA ->
                    copy(viewEffect = ConsentState.ViewEffect.OpenBottomSheet)
                ConsentClickableText.PRIVACY_CENTER ->
                    copy(viewEffect = OpenUrl(privacyCenterUrl))
                ConsentClickableText.DATA_ACCESS ->
                    copy(viewEffect = OpenUrl(dataPolicyUrl))
                null -> {
                    logger.error("Unrecognized clickable text: $tag")
                    this
                }
            }
        }
    }

    fun onViewEffectLaunched() {
        setState {
            copy(
                viewEffect = null
            )
        }
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

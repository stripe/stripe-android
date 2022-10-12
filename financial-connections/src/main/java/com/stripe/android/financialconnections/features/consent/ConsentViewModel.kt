package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.features.MarkdownParser
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.Body
import com.stripe.android.financialconnections.model.ConsentScreen
import com.stripe.android.financialconnections.model.DataDialog
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.sampleConsent
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val goNext: GoNext,
    private val getManifest: GetManifest,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val consent: ConsentScreen = sampleConsent.toHtml()
//            manualEntryEnabled = manifest.allowManualEntry,
//            manualEntryShowBusinessDaysNotice =
//                !manifest.customManualEntryHandling && manifest.manualEntryUsesMicrodeposits,
            consent
        }.execute { copy(consent = it) }
    }

    private fun ConsentScreen.toHtml(): ConsentScreen = ConsentScreen(
        title = MarkdownParser.toHtml(title),
        body = body.map {
            Body(
                iconUrl = it.iconUrl,
                text = MarkdownParser.toHtml(it.text),
                subtext = it.subtext?.let(MarkdownParser::toHtml)
            )
        },
        footerText = MarkdownParser.toHtml(footerText),
        buttonTitle = MarkdownParser.toHtml(buttonTitle),
        dataDialog = DataDialog(
            title = MarkdownParser.toHtml(dataDialog.title),
            body = dataDialog.body.map {
                Body(
                    iconUrl = it.iconUrl,
                    text = MarkdownParser.toHtml(it.text),
                    subtext = it.subtext?.let(MarkdownParser::toHtml)
                )
            },
            footerText = MarkdownParser.toHtml(dataDialog.footerText),
            buttonTitle = MarkdownParser.toHtml(dataDialog.buttonTitle),
        )
    )

    private fun logErrors() {
        onAsync(ConsentState::consent, onFail = {
            logger.error("Error retrieving consent content", it)
        })
        onAsync(ConsentState::acceptConsent, onFail = {
            logger.error("Error accepting consent", it)
        })
    }

    fun onContinueClick() {
        suspend {
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            goNext(updatedManifest.nextPane)
            Unit
        }.execute { copy(acceptConsent = it) }
    }

    fun onClickableTextClick(tag: String) {
        if (tag == "stripe://data-access-notice") {
            setState { copy(viewEffect = ConsentState.ViewEffect.OpenBottomSheet) }
        } else {
            setState { copy(viewEffect = OpenUrl(tag)) }
        }
//        when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
//            ConsentClickableText.TERMS ->
//                setState { copy(viewEffect = OpenUrl(tag)) }
//
//            ConsentClickableText.PRIVACY ->
//                setState { copy(viewEffect = OpenUrl(FinancialConnectionsUrls.StripePrivacyPolicy)) }
//
//            ConsentClickableText.DISCONNECT ->
//                setState { copy(viewEffect = OpenUrl(tag)) }
//
//            ConsentClickableText.DATA ->
//                setState { copy(viewEffect = ConsentState.ViewEffect.OpenBottomSheet) }
//
//            ConsentClickableText.PRIVACY_CENTER ->
//                setState { copy(viewEffect = OpenUrl(tag)) }
//
//            ConsentClickableText.DATA_ACCESS ->
//                setState { copy(viewEffect = OpenUrl(tag)) }
//
//            ConsentClickableText.MANUAL_ENTRY ->
//                navigationManager.navigate(NavigationDirections.manualEntry)
//
//            null -> logger.error("Unrecognized clickable text: $tag")
//        }
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

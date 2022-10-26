package com.stripe.android.financialconnections.features.consent

import android.net.Uri
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
import com.stripe.android.financialconnections.features.MarkdownParser
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
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
            MarkdownParser.toHtml(sync.text!!.consent!!)
        }.execute { copy(consent = it) }
    }

    private fun logErrors() {
        onAsync(
            ConsentState::consent,
            onSuccess = { eventTracker.track(FinancialConnectionsEvent.PaneLoaded(NextPane.CONSENT)) },
            onFail = { logger.error("Error retrieving consent content", it) }
        )
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
        if (URLUtil.isNetworkUrl(tag)) {
            kotlin.runCatching {
                Uri.parse(tag)
                    .getQueryParameter("eventName")
                    ?.let { logClick(it) }
                setState { copy(viewEffect = OpenUrl(tag)) }
            }
        } else when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
            ConsentClickableText.DATA -> {
                logClick("click.data_requested")
                setState { copy(viewEffect = ViewEffect.OpenBottomSheet) }
            }

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

package com.stripe.android.financialconnections.features.consent

import android.net.Uri
import android.webkit.URLUtil
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.features.MarkdownParser
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val goNext: GoNext,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    init {
        logErrors()
        suspend {
            val sync = getOrFetchSync()
            sync.text!!.consent!!.toHtml()
        }.execute { copy(consent = it) }
    }

    private fun ConsentPane.toHtml(): ConsentPane = ConsentPane(
        title = MarkdownParser.toHtml(title),
        body = ConsentPaneBody(
            bullets = body.bullets.map { bullet ->
                Bullet(
                    icon = bullet.icon,
                    content = MarkdownParser.toHtml(bullet.content),
                    title = bullet.title?.let { MarkdownParser.toHtml(it) }
                )
            }
        ),
        belowCta = belowCta?.let { MarkdownParser.toHtml(it) },
        aboveCta = MarkdownParser.toHtml(aboveCta),
        cta = MarkdownParser.toHtml(cta),
        dataAccessNotice = DataAccessNotice(
            title = MarkdownParser.toHtml(dataAccessNotice.title),
            body = DataAccessNoticeBody(
                bullets = dataAccessNotice.body.bullets.map { bullet ->
                    Bullet(
                        icon = bullet.icon,
                        content = MarkdownParser.toHtml(bullet.content),
                        title = bullet.title?.let { MarkdownParser.toHtml(it) }
                    )
                }
            ),
            learnMore = MarkdownParser.toHtml(dataAccessNotice.learnMore),
            cta = MarkdownParser.toHtml(dataAccessNotice.cta),
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
        if (URLUtil.isNetworkUrl(tag)) {
            kotlin.runCatching {
                Uri.parse(tag)
                    .getQueryParameter("eventName")
                    // TODO@carlosmuvi send event tracker!
                    ?.let { logger.debug("EVENT: $it") }
                setState { copy(viewEffect = OpenUrl(tag)) }
            }
        } else when (ConsentClickableText.values().firstOrNull { it.value == tag }) {
            ConsentClickableText.DATA ->
                setState { copy(viewEffect = ViewEffect.OpenBottomSheet) }

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

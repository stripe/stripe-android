package com.stripe.android.financialconnections.features.bankauthrepair

import android.webkit.URLUtil
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.CreateRepairSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ClickableText
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationRepairSession
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.repository.PartnerToCoreAuthsRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
internal class BankAuthRepairViewModel @Inject constructor(
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val uriUtils: UriUtils,
    private val createRepairSession: CreateRepairSession,
    private val updateLocalManifest: UpdateLocalManifest,
    private val partnerToCoreAuthsRepository: PartnerToCoreAuthsRepository,
    private val getManifest: GetManifest,
    private val getCachedAccounts: GetCachedAccounts,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {
    init {
        logErrors()
        observePayload()
        suspend {
            val selectedAccount = getCachedAccounts().first()
            val coreAuthorization = requireNotNull(partnerToCoreAuthsRepository.get())
                .getValue(selectedAccount.authorization)
            val repairSession = createRepairSession(coreAuthorization)
            updateLocalManifest { it.copy(activeInstitution = repairSession.institution) }
            Payload(
                authSession = repairSession.toAuthSession(),
                institution = requireNotNull(repairSession.institution),
                isStripeDirect = getManifest().isStripeDirect ?: false
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun FinancialConnectionsAuthorizationRepairSession.toAuthSession() =
        FinancialConnectionsAuthorizationSession(
            id = this.id,
            url = this.url,
            flow = this.flow,
            _isOAuth = this.isOAuth,
            nextPane = Pane.SUCCESS,
            display = this.display,
        )

    private fun observePayload() {
        onAsync(
            asyncProp = PartnerAuthState::payload,
            onSuccess = {
                // launch auth for non-OAuth (skip pre-pane).
                if (!it.authSession.isOAuth) launchAuthInBrowser()
            }
        )
    }

    private fun logErrors() {
        onAsync(
            PartnerAuthState::payload,
            onFail = {
                logger.error("Error fetching payload / posting AuthSession", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.BANK_AUTH_REPAIR, it))
            },
            onSuccess = { eventTracker.track(PaneLoaded(Pane.BANK_AUTH_REPAIR)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            launchAuthInBrowser()
        }
    }

    private suspend fun launchAuthInBrowser() {
        kotlin.runCatching { requireNotNull(awaitState().payload()?.authSession) }
            .onSuccess {
                it.url
                    ?.replaceFirst("stripe-auth://native-redirect/$applicationId/", "")
                    ?.let { setState { copy(viewEffect = OpenPartnerAuth(it)) } }
            }
            .onFailure {
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.BANK_AUTH_REPAIR, it))
                logger.error("failed retrieving active session from cache", it)
                setState { copy(authenticationStatus = Fail(it)) }
            }
    }

    fun onSelectAnotherBank() {
        navigationManager.tryNavigateTo(Destination.Reset(referrer = Pane.BANK_AUTH_REPAIR))
    }

    fun onWebAuthFlowFinished(
        webStatus: WebAuthFlowState
    ) {
        logger.debug("Web AuthFlow status received $webStatus")
        viewModelScope.launch {
            when (webStatus) {
                is WebAuthFlowState.Canceled -> {
                    onAuthCancelled(webStatus.url)
                }

                is WebAuthFlowState.Failed -> {
                    onAuthFailed(
                        url = webStatus.url,
                        message = webStatus.message,
                        reason = webStatus.reason
                    )
                }

                WebAuthFlowState.InProgress -> {
                    setState { copy(authenticationStatus = Loading()) }
                }

                is WebAuthFlowState.Success -> {
                    completeAuthorizationSession(webStatus.url)
                }

                WebAuthFlowState.Uninitialized -> {}
            }
        }
    }

    private fun onAuthFailed(
        url: String,
        message: String,
        reason: String?
    ) {
        // TODO handle auth failures
        logger.debug("Auth failed $message")
    }

    private fun onAuthCancelled(url: String?) {
        // TODO handle auth cancellations
        logger.debug("Auth cancelled")
    }

    private fun completeAuthorizationSession(url: String) {
        // TODO handle auth succeeding.
        logger.debug("Auth succeeded!")
    }

    fun onEnterDetailsManuallyClick() {
        navigationManager.tryNavigateTo(Destination.ManualEntry(referrer = Pane.BANK_AUTH_REPAIR))
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        // if clicked uri contains an eventName query param, track click event.
        viewModelScope.launch {
            uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
                eventTracker.track(
                    FinancialConnectionsEvent.Click(
                        eventName,
                        pane = Pane.BANK_AUTH_REPAIR
                    )
                )
            }
        }
        if (URLUtil.isNetworkUrl(uri)) {
            setState {
                copy(
                    viewEffect = OpenUrl(
                        uri,
                        Date().time
                    )
                )
            }
        } else {
            val managedUri = ClickableText.values()
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                ClickableText.DATA -> {
                    setState {
                        copy(
                            viewEffect = OpenBottomSheet(Date().time)
                        )
                    }
                }

                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    fun onViewEffectLaunched() {
        setState {
            copy(viewEffect = null)
        }
    }

    companion object : MavericksViewModelFactory<BankAuthRepairViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): BankAuthRepairViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .bankAuthRepairSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

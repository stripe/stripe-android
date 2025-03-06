package com.stripe.android.financialconnections.features.consent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupViewModel.Companion.PANE
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_NEXT_PANE_ON_DISABLE_NETWORKING
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkLoginWarmup
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.experimentAssignment
import com.stripe.android.financialconnections.utils.trackExposure
import com.stripe.android.model.EmailSource
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.util.Date

internal class ConsentViewModel @AssistedInject constructor(
    @Assisted initialState: ConsentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val logger: Logger,
    private val presentSheet: PresentSheet,
    private val lookupAccount: LookupAccount,
    private val isLinkWithStripe: IsLinkWithStripe,
    private val prefillDetails: ElementsSessionContext.PrefillDetails?,
) : FinancialConnectionsViewModel<ConsentState>(initialState, nativeAuthFlowCoordinator) {

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
                merchantLogos = sync.visual.merchantLogos,
                showAnimatedDots = sync.manifest.showAnimatedDots,
            )
        }.execute { copy(consent = it) }
    }

    override fun updateTopAppBar(state: ConsentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.CONSENT,
            hideStripeLogo = state.consent()?.shouldShowMerchantLogos ?: true,
            allowBackNavigation = true,
            error = state.consent.error,
        )
    }

    private fun logErrors() {
        onAsync(
            ConsentState::consent,
            onSuccess = { eventTracker.track(PaneLoaded(Pane.CONSENT)) },
            onFail = { logger.error("Error retrieving consent content", it) }
        )
        onAsync(ConsentState::acceptConsent, onFail = {
            eventTracker.logError(
                extraMessage = "Error accepting consent",
                error = it,
                logger = logger,
                pane = Pane.CONSENT
            )
        })
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(ConsentAgree)
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            FinancialConnections.emitEvent(Name.CONSENT_ACQUIRED)

            val destination = determineNavigationDestination(updatedManifest)
            navigationManager.tryNavigateTo(destination(referrer = Pane.CONSENT))

            updatedManifest
        }.execute { copy(acceptConsent = it) }
    }

    private suspend fun determineNavigationDestination(
        manifest: FinancialConnectionsSessionManifest,
    ): Destination {
        val defaultDestination = manifest.nextPane.destination

        val useManifestNextPane = !isLinkWithStripe() ||
            manifest.accountholderCustomerEmailAddress != null ||
            prefillDetails?.email == null

        if (useManifestNextPane) {
            return defaultDestination
        }

        val hasExistingAccount = hasExistingLinkAccount(manifest, prefillDetails.email)
        return if (hasExistingAccount) {
            NetworkingLinkLoginWarmup
        } else {
            defaultDestination
        }
    }

    private suspend fun hasExistingLinkAccount(
        manifest: FinancialConnectionsSessionManifest,
        email: String,
    ): Boolean {
        return runCatching {
            lookupAccount(
                pane = PANE,
                email = email,
                phone = null,
                phoneCountryCode = null,
                emailSource = EmailSource.CUSTOMER_OBJECT,
                sessionId = manifest.id,
                verifiedFlow = manifest.appVerificationEnabled,
            ).exists
        }.getOrDefault(false)
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        val date = Date()
        handleClickableUrl(
            currentPane = Pane.CONSENT,
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
                    navigationManager.tryNavigateTo(ManualEntry(referrer = Pane.CONSENT))
                },
                // Clicked on the "Manual entry" link on NME flows -> Navigate to the Link Login Warmup screen
                ConsentClickableText.LINK_LOGIN_WARMUP.value to {
                    navigationManager.tryNavigateTo(
                        NetworkingLinkLoginWarmup(
                            referrer = Pane.CONSENT,
                            extraArgs = mapOf(KEY_NEXT_PANE_ON_DISABLE_NETWORKING to it.nextPaneOrDrawerOnSecondaryCta)
                        )
                    )
                },
                // Surfaces where user has signed in to Link and then launches the auth flow.
                ConsentClickableText.LINK_ACCOUNT_PICKER.value to {
                    navigationManager.tryNavigateTo(
                        route = Destination.LinkAccountPicker(referrer = Pane.CONSENT)
                    )
                }
            )
        )
    }

    private fun presentDataAccessBottomSheet() {
        val dataAccessNotice = stateFlow.value.consent()?.consent?.dataAccessNotice ?: return
        presentSheet(
            content = DataAccess(dataAccessNotice),
            referrer = Pane.CONSENT,
        )
    }

    private fun presentLegalDetailsBottomSheet() {
        val notice = stateFlow.value.consent()?.consent?.legalDetailsNotice ?: return
        presentSheet(
            content = Legal(notice),
            referrer = Pane.CONSENT,
        )
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ConsentState): ConsentViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.consentViewModelFactory.create(ConsentState())
                }
            }
    }
}

private val FinancialConnectionsSessionManifest.showAnimatedDots: Boolean
    get() {
        val isInstantDebits = isLinkWithStripe ?: false
        return !isInstantDebits
    }

package com.stripe.android.financialconnections.features.success

import androidx.annotation.VisibleForTesting
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickDisconnectLink
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Complete
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.TextResource.PluralId
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getCachedAccounts: GetCachedAccounts,
    getManifest: GetManifest,
    private val saveToLinkWithStripeSucceeded: SaveToLinkWithStripeSucceededRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        observeAsyncs()
        suspend {
            val manifest = getManifest()
            val accounts = getCachedAccounts()
            val saveToLinkWithStripeSucceeded: Boolean? = saveToLinkWithStripeSucceeded.get()
            SuccessState.Payload(
                skipSuccessPane = manifest.skipSuccessPane ?: false,
                successMessage = getSuccessMessages(
                    isLinkWithStripe = manifest.isLinkWithStripe,
                    isNetworkingUserFlow = manifest.isNetworkingUserFlow,
                    saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded,
                    businessName = manifest.businessName,
                    connectedAccountName = manifest.connectedAccountName,
                    count = accounts.size
                ),
                accessibleData = AccessibleDataCalloutModel(
                    businessName = manifest.businessName,
                    permissions = manifest.permissions,
                    isNetworking = manifest.isNetworkingUserFlow == true && saveToLinkWithStripeSucceeded == true,
                    isStripeDirect = manifest.isStripeDirect ?: false,
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
                ),
                accounts = accounts,
                institution = manifest.activeInstitution!!,
                businessName = manifest.businessName,
                disconnectUrl = FinancialConnectionsUrlResolver.getDisconnectUrl(manifest),
                accountFailedToLinkMessage = getFailedToLinkMessage(
                    businessName = manifest.businessName,
                    saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded,
                    count = accounts.size
                )
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun observeAsyncs() {
        onAsync(
            SuccessState::payload,
            onFail = {
                logger.error("Error retrieving payload", it)
            },
            onSuccess = {
                if (it.skipSuccessPane.not()) {
                    eventTracker.track(PaneLoaded(Pane.SUCCESS))
                } else {
                    completeSession()
                }
            }
        )
        onAsync(
            SuccessState::completeSession,
            onSuccess = {
                // Complete session succeeds, finish the AuthFlow with the received session.
                eventTracker.track(
                    Complete(
                        connectedAccounts = it.accounts.data.count(),
                        exceptionExtraMessage = null,
                        exception = null
                    )
                )
                nativeAuthFlowCoordinator().emit(
                    Message.Finish(
                        Completed(
                            financialConnectionsSession = it,
                            token = it.parsedToken
                        )
                    )
                )
            },
            // Complete session fails, finish the AuthFlow with the received error.
            onFail = { error ->
                eventTracker.track(
                    Complete(
                        connectedAccounts = null,
                        exceptionExtraMessage = "Error completing session",
                        exception = error
                    )
                )
                logger.error("Error completing session", error)
                nativeAuthFlowCoordinator().emit(
                    Message.Finish(
                        Failed(error)
                    )
                )
            }
        )
    }

    @VisibleForTesting
    fun getFailedToLinkMessage(
        businessName: String?,
        saveToLinkWithStripeSucceeded: Boolean?,
        count: Int
    ): TextResource? = when {
        saveToLinkWithStripeSucceeded != false -> null
        businessName != null -> PluralId(
            value = R.plurals.stripe_success_networking_save_to_link_failed,
            count = count,
            args = listOf(businessName)
        )

        else -> PluralId(
            R.plurals.stripe_success_pane_networking_save_to_link_failed_no_business,
            count,
        )
    }

    @VisibleForTesting
    internal fun getSuccessMessages(
        isLinkWithStripe: Boolean?,
        isNetworkingUserFlow: Boolean?,
        saveToLinkWithStripeSucceeded: Boolean?,
        connectedAccountName: String?,
        businessName: String?,
        count: Int
    ): TextResource = when {
        isLinkWithStripe == true ||
            (isNetworkingUserFlow == true && saveToLinkWithStripeSucceeded == true) -> when {
            businessName != null && connectedAccountName != null -> PluralId(
                value = R.plurals.stripe_success_pane_link_with_connected_account_name,
                count = count,
                args = listOf(connectedAccountName, businessName)
            )

            businessName != null -> PluralId(
                value = R.plurals.stripe_success_pane_link_with_business_name,
                count = count,
                args = listOf(businessName)
            )

            else -> PluralId(
                R.plurals.stripe_success_pane_link_with_no_business_name,
                count,
            )
        }

        businessName != null && connectedAccountName != null -> PluralId(
            R.plurals.stripe_success_pane_has_connected_account_name,
            count = count,
            args = listOf(connectedAccountName, businessName)
        )

        businessName != null -> PluralId(
            R.plurals.stripe_success_pane_has_business_name,
            count,
            args = listOf(businessName)
        )

        else -> PluralId(R.plurals.stripe_success_pane_no_business_name, count)
    }

    fun onDoneClick() {
        viewModelScope.launch { eventTracker.track(ClickDone(Pane.SUCCESS)) }
        completeSession()
    }

    private fun completeSession() {
        suspend {
            completeFinancialConnectionsSession()
        }.execute { copy(completeSession = it) }
    }

    fun onLearnMoreAboutDataAccessClick() {
        viewModelScope.launch {
            eventTracker.track(ClickLearnMoreDataAccess(Pane.SUCCESS))
        }
    }

    fun onDisconnectLinkClick() {
        viewModelScope.launch {
            eventTracker.track(ClickDisconnectLink(Pane.SUCCESS))
        }
    }

    companion object : MavericksViewModelFactory<SuccessViewModel, SuccessState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SuccessState
        ): SuccessViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .successSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class SuccessState(
    val payload: Async<Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState {

    data class Payload(
        val accessibleData: AccessibleDataCalloutModel,
        val institution: FinancialConnectionsInstitution,
        val accounts: List<PartnerAccount>,
        val disconnectUrl: String,
        val businessName: String?,
        val skipSuccessPane: Boolean,
        val successMessage: TextResource,
        val accountFailedToLinkMessage: TextResource?
    )
}

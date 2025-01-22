package com.stripe.android.financialconnections.features.bankauthrepair

import android.os.Parcelable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RepairAuthorizationSession
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.utils.UriUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import javax.inject.Named

internal class BankAuthRepairViewModel @AssistedInject constructor(
    completeAuthorizationSession: CompleteAuthorizationSession,
    createAuthorizationSession: PostAuthorizationSession,
    cancelAuthorizationSession: CancelAuthorizationSession,
    retrieveAuthorizationSession: RetrieveAuthorizationSession,
    eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) applicationId: String,
    uriUtils: UriUtils,
    postAuthSessionEvent: PostAuthSessionEvent,
    getOrFetchSync: GetOrFetchSync,
    browserManager: BrowserManager,
    handleError: HandleError,
    navigationManager: NavigationManager,
    pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    logger: Logger,
    presentSheet: PresentSheet,
    @Assisted initialState: SharedPartnerAuthState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val pendingRepairRepository: CoreAuthorizationPendingNetworkingRepairRepository,
    private val repairAuthSession: RepairAuthorizationSession,
) : SharedPartnerAuthViewModel(
    completeAuthorizationSession,
    createAuthorizationSession,
    cancelAuthorizationSession,
    retrieveAuthorizationSession,
    eventTracker,
    applicationId,
    uriUtils,
    postAuthSessionEvent,
    getOrFetchSync,
    browserManager,
    handleError,
    navigationManager,
    pollAuthorizationSessionOAuthResults,
    logger,
    presentSheet,
    initialState,
    nativeAuthFlowCoordinator,
) {

    override suspend fun fetchPayload(sync: SynchronizeSessionResponse): Payload {
        val authorization = requireNotNull(pendingRepairRepository.get()?.coreAuthorization)
        val activeInstitution = requireNotNull(sync.manifest.activeInstitution)

        val authSession = repairAuthSession(authorization)

        return Payload(
            isStripeDirect = sync.manifest.isStripeDirect ?: false,
            institution = activeInstitution,
            authSession = authSession,
        )
    }

    @Parcelize
    data class Args(val pane: Pane) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(initialState: SharedPartnerAuthState): BankAuthRepairViewModel
    }

    internal companion object {
        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.bankAuthRepairViewModelFactory.create(SharedPartnerAuthState(Args(PANE)))
                }
            }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}

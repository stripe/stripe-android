package com.stripe.android.financialconnections.features.partnerauth

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
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import javax.inject.Named

internal class PartnerAuthViewModel @AssistedInject constructor(
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
        val manifest = sync.manifest
        val authSession = manifest.activeAuthSession ?: createAuthorizationSession(
            institution = requireNotNull(manifest.activeInstitution),
            sync = sync
        )
        return Payload(
            isStripeDirect = manifest.isStripeDirect ?: false,
            institution = requireNotNull(manifest.activeInstitution),
            authSession = authSession,
        )
    }

    @Parcelize
    data class Args(val inModal: Boolean, val pane: Pane) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(initialState: SharedPartnerAuthState): PartnerAuthViewModel
    }

    companion object {
        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent, args: Args): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.partnerAuthViewModelFactory.create(SharedPartnerAuthState(args))
                }
            }
    }
}

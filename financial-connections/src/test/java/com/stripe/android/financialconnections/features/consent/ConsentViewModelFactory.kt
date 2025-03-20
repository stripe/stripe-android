package com.stripe.android.financialconnections.features.consent

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.uicore.navigation.NavigationManager
import org.mockito.kotlin.mock

internal object ConsentViewModelFactory {

    fun create(
        initialState: ConsentState = ConsentState(),
        nativeAuthFlowCoordinator: NativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
        acceptConsent: AcceptConsent = mock(),
        getOrFetchSync: GetOrFetchSync = mock(),
        navigationManager: NavigationManager = TestNavigationManager(),
        eventTracker: FinancialConnectionsAnalyticsTracker = TestFinancialConnectionsAnalyticsTracker(),
        handleClickableUrl: HandleClickableUrl = mock(),
        presentSheet: PresentSheet = mock(),
        lookupAccount: LookupAccount = mock(),
        isLinkWithStripe: IsLinkWithStripe = mock(),
        prefillDetails: ElementsSessionContext.PrefillDetails? = null,
    ): ConsentViewModel {
        return ConsentViewModel(
            initialState = initialState,
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
            acceptConsent = acceptConsent,
            getOrFetchSync = getOrFetchSync,
            navigationManager = navigationManager,
            eventTracker = eventTracker,
            handleClickableUrl = handleClickableUrl,
            logger = Logger.noop(),
            presentSheet = presentSheet,
            lookupAccount = lookupAccount,
            isLinkWithStripe = isLinkWithStripe,
            prefillDetails = prefillDetails,
        )
    }
}

package com.stripe.android.financialconnections.features.accountupdate

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.AccountUpdateRequiredContentRepository
import javax.inject.Inject

internal interface PresentAccountUpdateRequiredSheet {
    operator fun invoke(state: AccountUpdateRequiredState.Payload, referrer: Pane)
}

internal class RealPresentAccountUpdateRequiredSheet @Inject constructor(
    private val navigationManager: NavigationManager,
    private val accountUpdateRequiredContentRepository: AccountUpdateRequiredContentRepository,
) : PresentAccountUpdateRequiredSheet {

    override fun invoke(state: AccountUpdateRequiredState.Payload, referrer: Pane) {
        accountUpdateRequiredContentRepository.set(state)
        val route = Destination.AccountUpdateRequired(referrer = referrer)
        navigationManager.tryNavigateTo(route)
    }
}

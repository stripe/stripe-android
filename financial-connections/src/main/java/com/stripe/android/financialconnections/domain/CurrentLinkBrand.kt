package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.repository.ConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ActivityRetainedScope
internal class CurrentLinkBrand @Inject constructor(
    private val initialState: FinancialConnectionsSheetNativeState,
    financialConnectionsManifestRepository: FinancialConnectionsManifestRepository,
    consumerSessionRepository: ConsumerSessionRepository,
) {
    val stateFlow: StateFlow<LinkBrand> =
        combineAsStateFlow(
            consumerSessionRepository.consumerSessionFlow.mapAsStateFlow { it?.linkBrand },
            financialConnectionsManifestRepository.syncFlow.mapAsStateFlow { it?.manifest?.linkBrand }
        ) { consumerLinkBrand, manifestLinkBrand ->
            consumerLinkBrand ?: manifestLinkBrand ?: initialState.linkBrand
        }

    operator fun invoke(): LinkBrand {
        return stateFlow.value
    }
}

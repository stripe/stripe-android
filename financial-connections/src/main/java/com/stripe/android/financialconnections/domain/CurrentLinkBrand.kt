package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.repository.ConsumerSessionRepository
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ActivityRetainedScope
internal class CurrentLinkBrand @Inject constructor(
    private val initialState: FinancialConnectionsSheetNativeState,
    private val consumerSessionRepository: ConsumerSessionRepository,
) {
    val stateFlow: StateFlow<LinkBrand> =
        consumerSessionRepository.consumerSessionFlow.mapAsStateFlow { consumerSession ->
            consumerSession?.linkBrand ?: initialState.linkBrand
        }
}

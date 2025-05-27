package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.LinkAccountInfo
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccountInfo: StateFlow<LinkAccountInfo> = combineAsStateFlow(
        savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_STATE, null),
        savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_UPDATE_REASON, null),
        ::LinkAccountInfo
    )

    fun set(info: LinkAccountInfo) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = info.linkAccount
        savedStateHandle[LINK_ACCOUNT_HOLDER_UPDATE_REASON] = info.lastUpdateReason
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
        private const val LINK_ACCOUNT_HOLDER_UPDATE_REASON = "LINK_ACCOUNT_HOLDER_UPDATE_REASON"
    }
}

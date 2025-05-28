package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccountInfo: StateFlow<LinkAccountUpdate.Value> = combineAsStateFlow(
        savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_STATE, null),
        savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_UPDATE_REASON, null)
    ) { a, b -> LinkAccountUpdate.Value(a, b) }

    fun set(info: LinkAccountUpdate.Value) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = info.account
        savedStateHandle[LINK_ACCOUNT_HOLDER_UPDATE_REASON] = info.lastUpdateReason
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
        private const val LINK_ACCOUNT_HOLDER_UPDATE_REASON = "LINK_ACCOUNT_HOLDER_UPDATE_REASON"
    }
}

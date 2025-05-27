package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentsheet.LinkAccountInfo
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccount: StateFlow<LinkAccount?> = savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_STATE, null)
    val updateReason: StateFlow<LinkAccountUpdate.Value.UpdateReason?> = savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_UPDATE_REASON, null)
    val linkAccountInfo: StateFlow<LinkAccountInfo> = combineAsStateFlow(linkAccount, updateReason, ::LinkAccountInfo)

    fun set(account: LinkAccount?, updateReason: LinkAccountUpdate.Value.UpdateReason? = null) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = account
        savedStateHandle[LINK_ACCOUNT_HOLDER_UPDATE_REASON] = updateReason
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
        private const val LINK_ACCOUNT_HOLDER_UPDATE_REASON = "LINK_ACCOUNT_HOLDER_UPDATE_REASON"
    }
}

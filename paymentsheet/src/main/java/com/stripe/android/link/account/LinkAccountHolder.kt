package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason
import com.stripe.android.link.model.LinkAccount
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccount: StateFlow<LinkAccount?> = savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_STATE, null)
    val updateReason: StateFlow<UpdateReason?> = savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_UPDATE_REASON, null)

    fun set(account: LinkAccount?, updateReason: UpdateReason? = null) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = account
        savedStateHandle[LINK_ACCOUNT_HOLDER_UPDATE_REASON] = updateReason
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
        private const val LINK_ACCOUNT_HOLDER_UPDATE_REASON = "LINK_ACCOUNT_HOLDER_UPDATE_REASON"
    }
}

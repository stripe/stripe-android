package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkAccountUpdate
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccountInfo: StateFlow<LinkAccountUpdate.Value> = savedStateHandle
        .getStateFlow(LINK_ACCOUNT_HOLDER_STATE, LinkAccountUpdate.Value(null, null))

    fun set(info: LinkAccountUpdate.Value) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = info
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
    }
}

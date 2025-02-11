package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.model.LinkAccount
import kotlinx.coroutines.flow.StateFlow

internal class LinkAccountHolder(
    private val savedStateHandle: SavedStateHandle
) {
    val linkAccount: StateFlow<LinkAccount?> = savedStateHandle.getStateFlow(LINK_ACCOUNT_HOLDER_STATE, null)

    fun set(account: LinkAccount?) {
        savedStateHandle[LINK_ACCOUNT_HOLDER_STATE] = account
    }

    companion object {
        private const val LINK_ACCOUNT_HOLDER_STATE = "LINK_ACCOUNT_HOLDER_STATE"
    }
}

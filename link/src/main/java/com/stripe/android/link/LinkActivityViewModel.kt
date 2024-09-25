package com.stripe.android.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class LinkActivityViewModel : LinkViewModel<LinkState, LinkAction, LinkResult, LinkEffect>(
    initialState = LinkState,
) {
    override fun actionToResult(action: LinkAction): Flow<LinkResult> {
        // These are placeholder actions, they may be removed
        return when (action) {
            LinkAction.BackPressed -> handleBackPressed()
            LinkAction.WalletClicked -> handleWalletClicked()
        }
    }

    private fun handleBackPressed(): Flow<LinkResult> {
        return flowOf(LinkResult.SendEffect(LinkEffect.GoBack))
    }

    private fun handleWalletClicked(): Flow<LinkResult> {
        return flowOf(
            value = LinkResult.SendEffect(
                effect = LinkEffect.NavigateTo(
                    screen = LinkScreen.Wallet
                )
            )
        )
    }

    override fun resultToState(currentState: LinkState, result: LinkResult) = currentState

    override fun resultToEffect(result: LinkResult): LinkEffect? {
        return when (result) {
            is LinkResult.SendEffect -> result.effect
        }
    }

    internal class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkActivityViewModel() as T
        }
    }
}

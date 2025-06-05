package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Default implementation of [LinkEmbeddedInteractor] that handles 2FA verification.
 */
internal class DefaultLinkEmbeddedInteractor @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle
) : LinkEmbeddedInteractor {

    private val _state: StateFlow<LinkEmbeddedState> =
        savedStateHandle.getStateFlow(
            LINK_EMBEDDED_STATE_KEY, LinkEmbeddedState(
                verificationState = VerificationState.Loading
            )
        )

    override val state: StateFlow<LinkEmbeddedState> = _state

    /**
     * Sets up Link verification domain logic (should be called once when initializing)
     */
    override fun setup(paymentMethodMetadata: PaymentMethodMetadata) {
        coroutineScope.launch {
            val linkAccountManager = paymentMethodMetadata.linkAccountManager()
            val linkAccount = linkAccountManager?.linkAccountInfo?.value?.account
            if (linkAccountManager != null && linkAccount != null && linkAccount.existingLinkCustomer()) {
                // If the user is logged in and needs verification, start the verification process.
                updateState {
                    it.copy(
                        verificationState = VerificationState.Verifying(
                            viewState = linkAccount.initial()
                        )
                    )
                }
            } else {
                updateState { it.copy(verificationState = VerificationState.Resolved) }
            }
        }
    }

    fun LinkAccount.initial() = VerificationViewState(
        email = email,
        redactedPhoneNumber = redactedPhoneNumber,
        isProcessing = true,
        isSendingNewCode = false,
        didSendNewCode = false,
        isDialog = true,
        requestFocus = false,
        errorMessage = null
    )

    private fun updateState(block: (LinkEmbeddedState) -> LinkEmbeddedState) {
        val currentState = _state.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = block(currentState)
    }

    private fun LinkAccount.existingLinkCustomer(): Boolean {
        return when (accountStatus) {
            // No account or logged out
            AccountStatus.Error -> false
            AccountStatus.SignedOut -> false
            AccountStatus.Verified -> true
            AccountStatus.NeedsVerification -> true
            AccountStatus.VerificationStarted -> true
        }
    }


    private fun PaymentMethodMetadata.linkAccountManager(): LinkAccountManager? {
        val configuration = linkState?.configuration ?: return null
        val linkComponent = linkConfigurationCoordinator.getComponent(configuration)
        return linkComponent.linkAccountManager
    }

    companion object {
        private const val LINK_EMBEDDED_STATE_KEY = "LINK_EMBEDDED_STATE_KEY"
    }

    @AssistedFactory
    fun interface Factory {
        fun create(coroutineScope: CoroutineScope): DefaultLinkEmbeddedInteractor
    }
}
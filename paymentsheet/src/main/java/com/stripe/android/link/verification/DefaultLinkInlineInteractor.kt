package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.OTPSpec
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Default implementation of [LinkInlineInteractor] that handles 2FA verification.
 */
internal class DefaultLinkInlineInteractor @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle
) : LinkInlineInteractor {

    override val otpElement = OTPSpec.transform()

    override val state: StateFlow<LinkInlineState> = savedStateHandle.getStateFlow(
        key = LINK_EMBEDDED_STATE_KEY,
        initialValue = LinkInlineState(
            verificationState = VerificationState.Loading
        )
    )

    /**
     * Sets up Link verification domain logic (should be called once when initializing)
     */
    override fun setup(paymentMethodMetadata: PaymentMethodMetadata) {
        if (FeatureFlags.showInlineSignupInWalletButtons.isEnabled.not()) {
            // If the feature flag is disabled, do not start Link verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        val linkAccountManager = paymentMethodMetadata.linkAccountManager()
        if (linkAccountManager == null) {
            // If there is no Link account manager, we don't need to handle verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        val linkAccount = linkAccountManager.linkAccountInfo.value.account
        if (linkAccount == null || linkAccount.accountStatus != AccountStatus.NeedsVerification) {
            // If there is no Link account or verification is not needed, don't start verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        coroutineScope.launch {
            linkAccountManager.startVerification()
            updateState { it.copy(verificationState = linkAccount.initial2FAState()) }
        }
    }

    private fun LinkAccount.initial2FAState() = VerificationState.Render2FA(
        VerificationViewState(
            email = email,
            redactedPhoneNumber = redactedPhoneNumber,
            isProcessing = false,
            isSendingNewCode = false,
            didSendNewCode = false,
            isDialog = true,
            requestFocus = false,
            errorMessage = null
        )
    )

    private fun updateState(block: (LinkInlineState) -> LinkInlineState) {
        val currentState = state.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = block(currentState)
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
        fun create(
            coroutineScope: CoroutineScope
        ): DefaultLinkInlineInteractor
    }
}

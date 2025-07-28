package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.ui.wallet.toDefaultPaymentUI
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.link.verification.VerificationState.Render2FA
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController.Companion.WALLETS_BUTTON_LINK_LAUNCHER
import com.stripe.android.ui.core.elements.OTPSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Default implementation of [LinkInlineInteractor] that handles 2FA verification.
 */
@Singleton
internal class DefaultLinkInlineInteractor @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    @Named(WALLETS_BUTTON_LINK_LAUNCHER) private val linkLauncher: LinkPaymentLauncher,
    private val logger: Logger,
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
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration
        if (linkConfiguration == null) {
            // If there is no Link account manager, we don't need to handle verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        if (linkConfigurationCoordinator.linkGate(linkConfiguration).useInlineOtpInWalletButtons.not()) {
            // If the feature flag is disabled, do not start Link verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        val linkAccountManager = linkConfigurationCoordinator
            .getComponent(linkConfiguration).linkAccountManager
        val linkAccount = linkAccountManager.linkAccountInfo.value.account
        if (linkAccount == null || linkAccount.accountStatus != AccountStatus.NeedsVerification) {
            // If there is no Link account or verification is not needed, don't start verification.
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
            return
        }

        updateState { it.copy(verificationState = linkAccount.initial2FAState(linkConfiguration)) }
        observeOtp(linkAccountManager)
        startVerification()
    }

    fun observeOtp(linkAccountManager: LinkAccountManager) {
        // Setup OTP completion handling using shared OTP element
        coroutineScope.launch {
            otpElement.otpCompleteFlow.collect { code ->
                val verificationState = state.value.verificationState
                if (verificationState is Render2FA && verificationState.viewState.isProcessing.not()) {
                    // Update to processing state
                    update2FAState { viewState ->
                        viewState.copy(
                            isProcessing = true,
                            errorMessage = null
                        )
                    }
                    // confirm verification
                    val result: Result<LinkAccount> = linkAccountManager.confirmVerification(code)
                    onConfirmationResult(verificationState, result)
                }
            }
        }
    }

    fun onConfirmationResult(
        verificationState: Render2FA,
        result: Result<LinkAccount>
    ) {
        result
            .onSuccess {
                val accountManager = verificationState.linkAccountManager()
                linkLauncher.present(
                    configuration = verificationState.linkConfiguration,
                    linkAccountInfo = accountManager.linkAccountInfo.value,
                    launchMode = LinkLaunchMode.PaymentMethodSelection(null),
                    useLinkExpress = true
                )
                // No UI changes - keep the 2FA until we get a result from the Link payment selection flow.
            }.onFailure { error ->
                update2FAState { viewState ->
                    viewState.copy(
                        isProcessing = false,
                        errorMessage = error.errorMessage
                    )
                }
            }
    }

    private fun LinkAccount.initial2FAState(linkConfiguration: LinkConfiguration) = Render2FA(
        linkConfiguration = linkConfiguration,
        viewState = VerificationViewState(
            email = email,
            redactedPhoneNumber = redactedPhoneNumber,
            isProcessing = false,
            isSendingNewCode = false,
            didSendNewCode = false,
            isDialog = true,
            requestFocus = false,
            errorMessage = null,
            defaultPayment = displayablePaymentDetails?.toDefaultPaymentUI(
                linkConfiguration.enableDisplayableDefaultValuesInEce
            )
        )
    )

    private fun updateState(block: (LinkInlineState) -> LinkInlineState) {
        val currentState = state.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = block(currentState)
    }

    private fun update2FAState(
        block: (VerificationViewState) -> VerificationViewState,
    ) {
        val currentState = state.value.verificationState
        if (currentState is Render2FA) {
            val newState = currentState.copy(viewState = block(currentState.viewState))
            updateState { it.copy(verificationState = newState) }
        } else {
            logger.error(
                "Expected Render2FA state but found ${currentState::class.simpleName}. Resetting to RenderButton."
            )
            updateState { it.copy(verificationState = VerificationState.RenderButton) }
        }
    }

    private fun Render2FA.linkAccountManager(): LinkAccountManager {
        return linkConfigurationCoordinator.getComponent(linkConfiguration).linkAccountManager
    }

    fun onLinkResult() {
        // Regardless of the Link result, the user completed verification,
        // so we can reset the verification state to RenderButton.
        updateState { it.copy(verificationState = VerificationState.RenderButton) }
    }

    override fun resendCode() {
        otpElement.controller.reset()
        update2FAState { viewState ->
            viewState.copy(
                isSendingNewCode = true,
                errorMessage = null
            )
        }
        startVerification()
    }

    override fun didShowCodeSentNotification() {
        update2FAState { viewState ->
            viewState.copy(didSendNewCode = false)
        }
    }

    private fun startVerification() {
        update2FAState { viewState ->
            viewState.copy(errorMessage = null)
        }

        coroutineScope.launch {
            val currentState = state.value.verificationState
            if (currentState is Render2FA) {
                val linkAccountManager = currentState.linkAccountManager()
                val result = linkAccountManager.startVerification()
                val error = result.exceptionOrNull()

                update2FAState { viewState ->
                    viewState.copy(
                        isSendingNewCode = false,
                        didSendNewCode = viewState.isSendingNewCode && error == null,
                        errorMessage = error?.errorMessage,
                    )
                }
            }
        }
    }

    companion object {
        private const val LINK_EMBEDDED_STATE_KEY = "LINK_EMBEDDED_STATE_KEY"
    }
}

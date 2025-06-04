package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.AccountStatus.NeedsVerification
import com.stripe.android.link.model.AccountStatus.VerificationStarted
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.link.verification.VerificationState.Verifying
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.ui.core.elements.OTPSpec
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Manages Link verification state and domain logic using unified LinkEmbeddedState
 */
internal class LinkEmbeddedInteractor @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger
) {

    internal val otpElement = OTPSpec.transform()

    private val linkEmbeddedState: StateFlow<LinkEmbeddedState> =
        savedStateHandle.getStateFlow(LINK_EMBEDDED_STATE_KEY, LinkEmbeddedState())

    val state: StateFlow<LinkEmbeddedState> = linkEmbeddedState

    /**
     * Sets up Link verification domain logic (should be called once when initializing)
     */
    fun setup(
        paymentMethodMetadata: PaymentMethodMetadata,
        onVerificationSucceeded: (defaultPaymentMethod: LinkPaymentMethod?) -> Unit
    ) {
        // Do not initialize if Link is not available
        if (paymentMethodMetadata.availableWallets.contains(WalletType.Link).not()) return
        val linkAccountManager = paymentMethodMetadata.linkAccountManager() ?: return

        updateState {
            it.copy(verificationState = VerificationState.Loading)
        }

        if (paymentMethodMetadata.existingLinkCustomer()) {
            setupLinkVerification(
                linkAccountManager = linkAccountManager,
                paymentMethodMetadata = paymentMethodMetadata,
                onVerificationSucceeded = onVerificationSucceeded
            )
        } else {
            // If no existing Link customer, no verification needed.
            updateState { it.copy(verificationState = VerificationState.Resolved) }
        }
    }

    private fun setupLinkVerification(
        linkAccountManager: LinkAccountManager,
        paymentMethodMetadata: PaymentMethodMetadata,
        onVerificationSucceeded: (LinkPaymentMethod?) -> Unit
    ) {
        // Setup OTP completion handling using shared OTP element
        coroutineScope.launch {
            otpElement.otpCompleteFlow.collect { code ->
                onOtpCompleted(
                    paymentMethodMetadata = paymentMethodMetadata,
                    linkAccountManager = linkAccountManager,
                    code = code,
                    onVerificationSucceeded = onVerificationSucceeded
                )
            }
        }
        coroutineScope.launch {
            linkAccountManager.linkAccountInfo.map { it.account }.collect { account ->
                when (account?.accountStatus) {
                    AccountStatus.Verified -> {
                        logger.debug("Verified")
                        updateState { it.copy(verificationState = VerificationState.Resolved) }
                    }
                    NeedsVerification -> {
                        logger.debug("NeedsVerification")
                        linkAccountManager.startVerification()
                    }
                    VerificationStarted -> {
                        logger.debug("VerificationStarted")
                        updateState { it.copy(verificationState = Verifying(initialVerificationState(account))) }
                    }
                    null,
                    AccountStatus.SignedOut,
                    AccountStatus.Error -> {
                        logger.debug("No verification needed or error")
                        updateState { it.copy(verificationState = VerificationState.Resolved) }
                    }
                }
            }
        }
    }

    private fun initialVerificationState(account: LinkAccount): VerificationViewState = VerificationViewState(
        isProcessing = false,
        requestFocus = true,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = account.redactedPhoneNumber,
        email = account.email,
        isDialog = false
    )

    private suspend fun onOtpCompleted(
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountManager: LinkAccountManager,
        code: String,
        onVerificationSucceeded: (defaultPaymentMethod: LinkPaymentMethod?) -> Unit
    ) {
        val verificationState = linkEmbeddedState.value.verificationState
        if (verificationState is Verifying && verificationState.viewState.isProcessing.not()) {
            // Update to processing state
            updateState {
                it.copy(
                    verificationState = Verifying(
                        verificationState.viewState.copy(
                            isProcessing = true,
                            errorMessage = null
                        )
                    )
                )
            }

            linkAccountManager.confirmVerification(code)
                .fold(
                    onSuccess = { updatedLinkAccount ->
                        onVerificationSuccess(
                            paymentMethodMetadata = paymentMethodMetadata,
                            linkAccountManager = linkAccountManager,
                            onVerificationSucceeded = onVerificationSucceeded
                        )
                    },
                    onFailure = { error ->
                        onVerificationError(
                            verificationState = verificationState,
                            error = error
                        )
                    }
                )
        }
    }

    private fun onVerificationError(
        verificationState: Verifying,
        error: Throwable
    ) {
        otpElement.controller.reset()
        updateState {
            it.copy(
                verificationState = Verifying(
                    verificationState.viewState.copy(
                        isProcessing = false,
                        errorMessage = error.errorMessage
                    )
                )
            )
        }
    }

    private suspend fun onVerificationSuccess(
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountManager: LinkAccountManager,
        onVerificationSucceeded: (LinkPaymentMethod?) -> Unit
    ) {
        // Fetch payment details and find default payment method
        val payment = fetchDefaultPaymentMethod(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountManager = linkAccountManager
        )
        updateState { it.copy(preservedPaymentMethod = payment) }
        onVerificationSucceeded(payment)
    }

    private suspend fun fetchDefaultPaymentMethod(
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountManager: LinkAccountManager,
    ): LinkPaymentMethod? {
        // Fetch payment details for common payment method types
        val paymentMethodTypes = paymentMethodMetadata.stripeIntent.paymentMethodTypes
        val result = linkAccountManager.listPaymentDetails(paymentMethodTypes.toSet())

        return result.fold(
            onSuccess = { consumerPaymentDetails ->
                // Find the default payment method
                val defaultPaymentDetails = consumerPaymentDetails.paymentDetails.firstOrNull { it.isDefault }
                return defaultPaymentDetails?.let { paymentDetails ->
                    LinkPaymentMethod.ConsumerPaymentDetails(
                        details = paymentDetails,
                        collectedCvc = null
                    )
                }
            },
            onFailure = {
                null
            }
        )
    }

    /**
     * Creates a Link PaymentSelection with the preserved selectedPaymentMethod
     * This should be called when user re-selects Link after switching to other payment methods
     */
    fun createLinkSelection(): PaymentSelection.Link {
        return PaymentSelection.Link()
    }

    private fun updateState(block: (LinkEmbeddedState) -> LinkEmbeddedState) {
        val currentState = linkEmbeddedState.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = block(currentState)
    }

    private fun PaymentMethodMetadata.linkAccountManager(): LinkAccountManager? {
        val configuration = linkState?.configuration ?: return null
        val linkComponent = linkConfigurationCoordinator.getComponent(configuration)
        return linkComponent.linkAccountManager
    }

    private fun PaymentMethodMetadata.existingLinkCustomer(): Boolean {
        return when (linkState?.loginState) {
            // No account or logged out
            null, LinkState.LoginState.LoggedOut -> false
            LinkState.LoginState.LoggedIn, LinkState.LoginState.NeedsVerification -> true
        }
    }

    companion object {
        private const val LINK_EMBEDDED_STATE_KEY = "LINK_EMBEDDED_STATE_KEY"
    }

    /**
     * Factory for creating instances of [LinkEmbeddedInteractor] with assisted injection
     */
    @AssistedFactory
    fun interface Factory {
        fun create(coroutineScope: CoroutineScope): LinkEmbeddedInteractor
    }
}

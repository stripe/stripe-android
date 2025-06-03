package com.stripe.android.link.verification

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.AccountStatus.NeedsVerification
import com.stripe.android.link.model.AccountStatus.VerificationStarted
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
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
internal class LinkEmbeddedManager @AssistedInject constructor(
    private val linkAccountHolder: LinkAccountHolder,
    @Assisted private val coroutineScope: CoroutineScope,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
) {

    internal val otpElement = OTPSpec.transform()

    private val linkEmbeddedState: StateFlow<LinkEmbeddedState> =
        savedStateHandle.getStateFlow(LINK_EMBEDDED_STATE_KEY, LinkEmbeddedState())

    /**
     * Exposes the unified Link embedded state with the shared OTP element
     */
    val state: StateFlow<LinkEmbeddedState> = linkEmbeddedState

    /**
     * Sets up Link verification domain logic (should be called once when initializing)
     */
    fun setup(
        paymentMethodMetadata: PaymentMethodMetadata,
        onVerificationSucceeded: (defaultPaymentMethod: LinkPaymentMethod?) -> Unit
    ) {
        val linkAccountManager = linkAccountManager(paymentMethodMetadata) ?: return

        // Setup verification flow with Link account state monitoring
        coroutineScope.launch {
            linkAccountHolder.linkAccountInfo.map { it.account }.collect { account ->
                when (account?.accountStatus) {
                    AccountStatus.Verified -> {
                        Log.d("STATUS", "Verified")
                        markVerificationCompleted()
                    }
                    NeedsVerification -> {
                        Log.d("STATUS", "NeedsVerification")
                        linkAccountManager.startVerification()
                    }
                    VerificationStarted -> {
                        Log.d("STATUS", "VerificationStarted")
                        updateVerificationState(initialState(account))
                    }
                    null,
                    AccountStatus.SignedOut,
                    AccountStatus.Error -> {
                        Log.d("STATUS", "No verification needed or error")
                    }
                }
            }
        }

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
    }

    private fun initialState(linkAccount: LinkAccount) = VerificationViewState(
        isProcessing = false,
        requestFocus = true,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = linkAccount.redactedPhoneNumber,
        email = linkAccount.email,
        isDialog = false
    )

    private fun linkAccountManager(paymentMethodMetadata: PaymentMethodMetadata): LinkAccountManager? {
        val configuration = paymentMethodMetadata.linkState?.configuration ?: return null
        val linkComponent = linkConfigurationCoordinator.getComponent(configuration)
        return linkComponent.linkAccountManager
    }

    private suspend fun onOtpCompleted(
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountManager: LinkAccountManager,
        code: String,
        onVerificationSucceeded: (defaultPaymentMethod: LinkPaymentMethod?) -> Unit
    ) {
        val verificationState = linkEmbeddedState.value.verificationState

        if (verificationState != null && !verificationState.isProcessing) {
            // Update to processing state
            updateVerificationState(
                verificationState.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            )

            val result = linkAccountManager.confirmVerification(code)
            result.fold(
                onSuccess = { updatedLinkAccount ->
                    // Mark verification as completed
                    markVerificationCompleted()

                    linkAccountHolder.set(
                        LinkAccountUpdate.Value(
                            account = updatedLinkAccount,
                            lastUpdateReason = null
                        )
                    )
                    // Fetch payment details and find default payment method
                    val payment = fetchDefaultPaymentMethod(
                        paymentMethodMetadata = paymentMethodMetadata,
                        linkAccountManager = linkAccountManager
                    )
                    onVerificationSucceeded(payment)
                },
                onFailure = { error ->
                    otpElement.controller.reset()
                    updateVerificationState(
                        verificationState.copy(
                            isProcessing = false,
                            errorMessage = error.errorMessage
                        )
                    )
                }
            )
        }
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
                val defaultPaymentDetails = consumerPaymentDetails.paymentDetails
                    .firstOrNull { it.isDefault }

                val defaultPaymentMethod = defaultPaymentDetails?.let { paymentDetails ->
                    LinkPaymentMethod.ConsumerPaymentDetails(
                        details = paymentDetails,
                        collectedCvc = null
                    )
                }

                // Save the default payment method for preservation
                if (defaultPaymentMethod != null) {
                    preservePaymentMethod(defaultPaymentMethod)
                }
                defaultPaymentMethod
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
        return PaymentSelection.Link(
            selectedPayment = linkEmbeddedState.value.preservedPaymentMethod
        )
    }

    private fun updateVerificationState(verificationState: VerificationViewState?) {
        val currentState = linkEmbeddedState.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = currentState.copy(
            verificationState = verificationState
        )
    }

    private fun markVerificationCompleted() {
        val currentState = linkEmbeddedState.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = currentState.copy(
            verificationState = null,
        )
    }

    private fun preservePaymentMethod(linkPaymentMethod: LinkPaymentMethod?) {
        val currentState = linkEmbeddedState.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = currentState.copy(
            preservedPaymentMethod = linkPaymentMethod
        )
    }

    companion object {
        private const val LINK_EMBEDDED_STATE_KEY = "LINK_EMBEDDED_STATE_KEY"
    }

    /**
     * Factory for creating instances of [LinkEmbeddedManager] with assisted injection
     */
    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): LinkEmbeddedManager
    }
}
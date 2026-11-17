package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuth
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.ConfirmVerificationResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface EmbeddedConfirmationHelper {
    fun confirm()
}

@EmbeddedPaymentElementScope
internal class DefaultEmbeddedConfirmationHelper @Inject constructor(
    private val confirmationStarter: EmbeddedConfirmationStarter,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val eventReporter: EventReporter,
    private val embeddedResultCallbackHelper: EmbeddedResultCallbackHelper,
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
) : EmbeddedConfirmationHelper {
    init {
        confirmationStarter.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationStarter.result.collect { result ->
                eventReporter.reportPaymentResult(result, confirmationStateHolder.state?.selection)
                embeddedResultCallbackHelper.setResult(result.asEmbeddedResult())
            }
        }
    }

    override fun confirm() {
        linkInlineAuth.endTyping()
        if (linkInlineAuth.isPresentingDialog) {
            embeddedResultCallbackHelper.setResult(
                EmbeddedPaymentElement.Result.Failed(
                    IllegalStateException("confirm was called while Link verification is presented.")
                )
            )
            return
        }
        val selection = confirmationStateHolder.state?.selection
        if (selection is PaymentSelection.Link && selection.selectedPayment == null &&
            linkInlineAuth.verifiesBeforeConfirming
        ) {
            // The customer closed the inline Link verification card, so they verify in the Link dialog first,
            // with the code the card already sent.
            lifecycleOwner.lifecycleScope.launch {
                when (linkInlineAuth.verifyBeforeConfirming()) {
                    ConfirmVerificationResult.Verified,
                    ConfirmVerificationResult.Unavailable -> startConfirmation()
                    ConfirmVerificationResult.Canceled -> {
                        embeddedResultCallbackHelper.setResult(EmbeddedPaymentElement.Result.Canceled())
                    }
                }
            }
            return
        }
        startConfirmation()
    }

    private fun startConfirmation() {
        confirmationArgs()?.let { confirmationArgs ->
            confirmationStarter.start(confirmationArgs)
        } ?: run {
            embeddedResultCallbackHelper.setResult(
                EmbeddedPaymentElement.Result.Failed(IllegalStateException("Not in a state that's confirmable."))
            )
        }
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationState = confirmationStateHolder.state ?: return null
        val confirmationOption = confirmationState.selection?.toConfirmationOption(
            configuration = confirmationState.configuration.asCommonConfiguration(),
            linkConfiguration = confirmationState.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = confirmationState.paymentMethodMetadata.cardFundingFilter,
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = confirmationState.paymentMethodMetadata,
            statusBarColor = confirmationState.statusBarColor,
        )
    }
}

private fun ConfirmationHandler.Result.asEmbeddedResult(): EmbeddedPaymentElement.Result = when (this) {
    is ConfirmationHandler.Result.Canceled -> {
        EmbeddedPaymentElement.Result.Canceled()
    }
    is ConfirmationHandler.Result.Failed -> {
        EmbeddedPaymentElement.Result.Failed(cause)
    }
    is ConfirmationHandler.Result.Succeeded -> {
        EmbeddedPaymentElement.Result.Completed()
    }
}

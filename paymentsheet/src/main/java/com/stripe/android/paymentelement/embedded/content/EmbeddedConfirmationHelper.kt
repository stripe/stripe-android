package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.challenge.warmer.PassiveChallengeWarmer
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

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
    private val passiveChallengeWarmer: PassiveChallengeWarmer,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String
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

        setupPassiveChallengeWarmer()
    }

    override fun confirm() {
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
            passiveCaptchaParams = confirmationState.paymentMethodMetadata.passiveCaptchaParams
        ) ?: return null

        return ConfirmationHandler.Args(
            intent = confirmationState.paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            initializationMode = confirmationState.initializationMode,
            appearance = confirmationState.configuration.appearance,
            shippingDetails = confirmationState.configuration.shippingDetails,
        )
    }

    private fun setupPassiveChallengeWarmer() {
        passiveChallengeWarmer.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationStateHolder.stateFlow.mapNotNull {
                it?.paymentMethodMetadata?.passiveCaptchaParams
            }.collectLatest { passiveCaptchaParams ->
                passiveChallengeWarmer.start(
                    passiveCaptchaParams = passiveCaptchaParams,
                    publishableKey = publishableKeyProvider(),
                    productUsage = productUsage
                )
            }
        }
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

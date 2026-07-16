@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStarter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface CheckoutConfirmationHelper {
    fun confirm()
}

/**
 * Drives confirmation for [CheckoutPresenter], mirroring [com.stripe.android.paymentelement.embedded.content.
 * DefaultEmbeddedConfirmationHelper]. The current [CheckoutControllerState] is read from [stateHolder] (the
 * controller's single source of truth) to build [ConfirmationHandler.Args], the confirmation is started through the
 * shared [EmbeddedConfirmationStarter], and each terminal [ConfirmationHandler.Result] is mapped to a
 * [CheckoutController.Result] and delivered to the merchant's [CheckoutController.ResultCallback].
 */
internal class DefaultCheckoutConfirmationHelper @Inject constructor(
    private val confirmationStarter: EmbeddedConfirmationStarter,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val stateHolder: CheckoutControllerStateHolder,
    private val resultCallback: CheckoutController.ResultCallback,
    private val eventReporter: EventReporter,
) : CheckoutConfirmationHelper {
    init {
        confirmationStarter.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationStarter.result.collect { result ->
                eventReporter.reportPaymentResult(result, stateHolder.selection.value)
                // Let the merchant mutate/retry the session again once a non-successful attempt settles.
                if (result !is ConfirmationHandler.Result.Succeeded) {
                    setIntegrationLaunched(false)
                }
                resultCallback.onResult(result.asCheckoutResult())
            }
        }
    }

    override fun confirm() {
        val args = confirmationArgs() ?: run {
            resultCallback.onResult(
                CheckoutController.Result.Failed(IllegalStateException("Not in a state that's confirmable."))
            )
            return
        }
        // Block session mutations while confirmation is in flight (see CheckoutController.withCheckoutState).
        setIntegrationLaunched(true)
        confirmationStarter.start(args)
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val state = stateHolder.state ?: return null
        val metadata = state.paymentMethodMetadata
        val configuration = state.embeddedConfiguration.asCommonConfiguration()

        val confirmationOption = state.paymentSelection?.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = metadata.linkState?.configuration,
            cardFundingFilter = metadata.cardFundingFilter,
            googlePayDisplayItems = GooglePayDisplayItemsFactory.create(metadata),
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = metadata,
            ),
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = metadata,
        )
    }

    private fun setIntegrationLaunched(integrationLaunched: Boolean) {
        stateHolder.state = stateHolder.state?.copy(integrationLaunched = integrationLaunched)
    }
}

private fun ConfirmationHandler.Result.asCheckoutResult(): CheckoutController.Result = when (this) {
    is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
    is ConfirmationHandler.Result.Canceled -> CheckoutController.Result.Canceled()
    is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
}

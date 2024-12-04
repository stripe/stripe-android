package com.stripe.android.paymentelement.confirmation

import com.stripe.android.link.LinkIntentConfirmationHandler
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultLinkIntentConfirmationHandler @Inject constructor(
    private val confirmationHandler: ConfirmationHandler
) : LinkIntentConfirmationHandler {

    override val state: Flow<LinkIntentConfirmationHandler.State>
        get() {
            return confirmationHandler.state.map(::transformConfirmationHandlerState)
        }

    override suspend fun confirmIntent(
        intent: StripeIntent,
        params: PaymentMethodCreateParams
    ) {
        confirmationHandler.start(
            arguments = ConfirmationHandler.Args(
                intent = intent,
                confirmationOption = PaymentMethodConfirmationOption.New(
                    initializationMode = intent.initializationMode(),
                    shippingDetails = null,
                    createParams = params,
                    optionsParams = null,
                    shouldSave = false
                )
            )
        )
    }

    private fun transformConfirmationHandlerState(
        confirmationHandlerState: ConfirmationHandler.State
    ): LinkIntentConfirmationHandler.State {
        return when (confirmationHandlerState) {
            is ConfirmationHandler.State.Complete -> {
                when (val result = confirmationHandlerState.result) {
                    is ConfirmationHandler.Result.Canceled -> {
                        LinkIntentConfirmationHandler.State.Cancelled
                    }
                    is ConfirmationHandler.Result.Failed -> {
                        LinkIntentConfirmationHandler.State.Failed(
                            cause = result.cause,
                            message = result.message
                        )
                    }
                    is ConfirmationHandler.Result.Succeeded -> {
                        LinkIntentConfirmationHandler.State.Success
                    }
                }
            }
            is ConfirmationHandler.State.Confirming -> {
                LinkIntentConfirmationHandler.State.Idle
            }
            ConfirmationHandler.State.Idle -> {
                LinkIntentConfirmationHandler.State.Idle
            }
        }
    }

    private fun StripeIntent.initializationMode(): PaymentElementLoader.InitializationMode {
        return when (this) {
            is PaymentIntent -> {
                PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = clientSecret ?: ""
                )
            }
            is SetupIntent -> PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = clientSecret ?: ""
            )
        }
    }
}
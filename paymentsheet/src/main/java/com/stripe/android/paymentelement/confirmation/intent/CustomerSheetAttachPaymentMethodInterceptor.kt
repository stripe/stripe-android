package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.coroutines.Single
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.fold
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import javax.inject.Inject

internal class CustomerSheetAttachPaymentMethodInterceptor(
    private val paymentMethodDataSourceProvider: Single<CustomerSheetPaymentMethodDataSource>,
    private val logger: Logger,
) : IntentConfirmationInterceptor {
    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        val error = IllegalStateException(
            "Cannot use CustomerSheetAttachPaymentMethodInterceptor with new payment methods!"
        )

        return ConfirmationDefinition.Action.Fail(
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            cause = error,
            message = error.stripeErrorMessage()
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        if (intent !is SetupIntent) {
            val error = IllegalStateException("Cannot confirm non setup intents with Customer Sheet!")

            return ConfirmationDefinition.Action.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        }

        return paymentMethodDataSourceProvider.await().attachPaymentMethod(confirmationOption.paymentMethod.id)
            .fold(
                onSuccess = {
                    ConfirmationDefinition.Action.Complete(
                        intent = intent.copy(paymentMethod = confirmationOption.paymentMethod),
                        deferredIntentConfirmationType = null,
                        completedFullPaymentFlow = true,
                    )
                },
                onFailure = { cause, displayMessage ->
                    logger.error(
                        msg = "Failed to attach payment method ${confirmationOption.paymentMethod.id} to customer",
                        t = cause,
                    )

                    ConfirmationDefinition.Action.Fail(
                        cause = cause,
                        message = displayMessage?.resolvableString ?: cause.stripeErrorMessage(),
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )
                }
            )
    }

    interface Factory {
        fun create(): IntentConfirmationInterceptor
    }
}

internal class DefaultCustomerSheetAttachPaymentMethodInterceptorFactory @Inject constructor(
    private val logger: Logger,
) : CustomerSheetAttachPaymentMethodInterceptor.Factory {
    override fun create(): CustomerSheetAttachPaymentMethodInterceptor {
        return CustomerSheetAttachPaymentMethodInterceptor(
            paymentMethodDataSourceProvider = CustomerSheetHacks.paymentMethodDataSource,
            logger = logger,
        )
    }
}

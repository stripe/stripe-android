package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.coroutines.Single
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.fold
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import javax.inject.Inject

internal class CustomerSheetSetupIntentInterceptor(
    private val intentDataSourceProvider: Single<CustomerSheetIntentDataSource>,
    private val intentFirstConfirmationInterceptorFactory: IntentFirstConfirmationInterceptor.Factory,
    private val logger: Logger,
    private val clientAttributionMetadata: ClientAttributionMetadata,
) : IntentConfirmationInterceptor {
    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        return intentDataSourceProvider.await()
            .retrieveSetupIntentClientSecret()
            .fold(
                onSuccess = { clientSecret ->
                    intentFirstConfirmationInterceptorFactory
                        .create(clientSecret, clientAttributionMetadata)
                        .intercept(
                            intent = intent,
                            confirmationOption = PaymentMethodConfirmationOption.Saved(
                                paymentMethod = confirmationOption.paymentMethod,
                                optionsParams = null,
                            ),
                            shippingValues = null,
                        )
                },
                onFailure = { cause, displayMessage ->
                    logger.error(
                        msg = "Failed to attach payment method to SetupIntent: ${confirmationOption.paymentMethod}",
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

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        val error = IllegalStateException(
            "Cannot use CustomerSheetSetupIntentInterceptor with new payment methods!"
        )

        return ConfirmationDefinition.Action.Fail(
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            cause = error,
            message = error.stripeErrorMessage()
        )
    }

    interface Factory {
        fun create(
            clientAttributionMetadata: ClientAttributionMetadata
        ): IntentConfirmationInterceptor
    }
}

internal class DefaultCustomerSheetSetupIntentInterceptorFactory @Inject constructor(
    private val intentFirstConfirmationInterceptorFactory: IntentFirstConfirmationInterceptor.Factory,
    private val logger: Logger,
) : CustomerSheetSetupIntentInterceptor.Factory {
    override fun create(
        clientAttributionMetadata: ClientAttributionMetadata
    ): CustomerSheetSetupIntentInterceptor {
        return CustomerSheetSetupIntentInterceptor(
            intentDataSourceProvider = CustomerSheetHacks.intentDataSource,
            intentFirstConfirmationInterceptorFactory = intentFirstConfirmationInterceptorFactory,
            logger = logger,
            clientAttributionMetadata = clientAttributionMetadata,
        )
    }
}

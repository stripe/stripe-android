package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.coroutines.Single
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.fold
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import javax.inject.Inject

internal interface CustomerSheetSetupIntentInterceptor {
    suspend fun intercept(
        intent: SetupIntent,
        paymentMethod: PaymentMethod
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>

    interface Factory {
        fun create(
            clientAttributionMetadata: ClientAttributionMetadata
        ): CustomerSheetSetupIntentInterceptor
    }
}

internal class DefaultCustomerSheetSetupIntentInterceptor(
    private val intentDataSourceProvider: Single<CustomerSheetIntentDataSource>,
    private val intentFirstConfirmationInterceptorFactory: IntentFirstConfirmationInterceptor.Factory,
    private val logger: Logger,
    private val clientAttributionMetadata: ClientAttributionMetadata,
) : CustomerSheetSetupIntentInterceptor {
    override suspend fun intercept(
        intent: SetupIntent,
        paymentMethod: PaymentMethod
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
                                paymentMethod = paymentMethod,
                                optionsParams = null,
                            ),
                            shippingValues = null,
                        )
                },
                onFailure = { cause, displayMessage ->
                    logger.error(
                        msg = "Failed to attach payment method to SetupIntent: $paymentMethod",
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
}

internal class DefaultCustomerSheetSetupIntentInterceptorFactory @Inject constructor(
    private val intentFirstConfirmationInterceptorFactory: IntentFirstConfirmationInterceptor.Factory,
    private val logger: Logger,
) : CustomerSheetSetupIntentInterceptor.Factory {
    override fun create(
        clientAttributionMetadata: ClientAttributionMetadata
    ): CustomerSheetSetupIntentInterceptor {
        return DefaultCustomerSheetSetupIntentInterceptor(
            intentDataSourceProvider = CustomerSheetHacks.intentDataSource,
            intentFirstConfirmationInterceptorFactory = intentFirstConfirmationInterceptorFactory,
            logger = logger,
            clientAttributionMetadata = clientAttributionMetadata,
        )
    }
}

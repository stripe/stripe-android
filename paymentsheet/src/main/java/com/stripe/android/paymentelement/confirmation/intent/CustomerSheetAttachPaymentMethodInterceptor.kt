package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.coroutines.Single
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.fold
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import javax.inject.Inject

internal interface CustomerSheetAttachPaymentMethodInterceptor {
    suspend fun intercept(
        intent: SetupIntent,
        paymentMethod: PaymentMethod,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>

    interface Factory {
        fun create(): CustomerSheetAttachPaymentMethodInterceptor
    }
}

internal class DefaultCustomerSheetAttachPaymentMethodInterceptor(
    private val paymentMethodDataSourceProvider: Single<CustomerSheetPaymentMethodDataSource>,
    private val logger: Logger,
) : CustomerSheetAttachPaymentMethodInterceptor {
    override suspend fun intercept(
        intent: SetupIntent,
        paymentMethod: PaymentMethod,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        return paymentMethodDataSourceProvider.await().attachPaymentMethod(paymentMethod.id)
            .fold(
                onSuccess = {
                    ConfirmationDefinition.Action.Complete(
                        intent = intent.copy(paymentMethod = paymentMethod),
                        completedFullPaymentFlow = true,
                    )
                },
                onFailure = { cause, displayMessage ->
                    logger.error(
                        msg = "Failed to attach payment method ${paymentMethod.id} to customer",
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

internal class DefaultCustomerSheetAttachPaymentMethodInterceptorFactory @Inject constructor(
    private val logger: Logger,
) : CustomerSheetAttachPaymentMethodInterceptor.Factory {
    override fun create(): CustomerSheetAttachPaymentMethodInterceptor {
        return DefaultCustomerSheetAttachPaymentMethodInterceptor(
            paymentMethodDataSourceProvider = CustomerSheetHacks.paymentMethodDataSource,
            logger = logger,
        )
    }
}

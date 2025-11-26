package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject

internal class CustomerSheetConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    @Assisted private val integrationMetadata: IntegrationMetadata.CustomerSheet,
    private val setupIntentInterceptorFactory: CustomerSheetSetupIntentInterceptor.Factory,
    private val attachPaymentMethodInterceptorFactory: CustomerSheetAttachPaymentMethodInterceptor.Factory,
) : IntentConfirmationInterceptor {
    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        val error = IllegalStateException(
            "Cannot use CustomerSheetConfirmationInterceptor with new payment methods!"
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
        return when (integrationMetadata.attachmentStyle) {
            IntegrationMetadata.CustomerSheet.AttachmentStyle.SetupIntent -> {
                val setupIntentInterceptor = setupIntentInterceptorFactory.create(clientAttributionMetadata)

                setupIntentInterceptor.intercept(
                    intent = intent,
                    confirmationOption = confirmationOption,
                    shippingValues = null,
                )
            }
            IntegrationMetadata.CustomerSheet.AttachmentStyle.CreateAttach -> {
                val attachPaymentMethodInterceptor = attachPaymentMethodInterceptorFactory.create()

                attachPaymentMethodInterceptor.intercept(
                    intent = intent,
                    confirmationOption = confirmationOption,
                    shippingValues = null,
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            clientAttributionMetadata: ClientAttributionMetadata,
            integrationMetadata: IntegrationMetadata.CustomerSheet,
        ): CustomerSheetConfirmationInterceptor
    }
}

internal class CustomerSheetIntentConfirmationInterceptorFactory @Inject constructor(
    private val customerSheetConfirmationInterceptor: CustomerSheetConfirmationInterceptor.Factory
) : IntentConfirmationInterceptor.Factory {
    override suspend fun create(
        integrationMetadata: IntegrationMetadata,
        customerId: String?,
        ephemeralKeySecret: String?,
        clientAttributionMetadata: ClientAttributionMetadata
    ): IntentConfirmationInterceptor {
        return when (integrationMetadata) {
            is IntegrationMetadata.CustomerSheet -> {
                customerSheetConfirmationInterceptor.create(clientAttributionMetadata, integrationMetadata)
            }
            else -> {
                throw IllegalStateException(
                    "${integrationMetadata::class.java.name} is not supported by Customer Sheet!"
                )
            }
        }
    }
}

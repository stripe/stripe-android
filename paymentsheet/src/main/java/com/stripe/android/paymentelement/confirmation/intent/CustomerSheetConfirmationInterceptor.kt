package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.customersheet.util.isUnverifiedUSBankAccount
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
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
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val setupIntentInterceptorFactory: CustomerSheetSetupIntentInterceptor.Factory,
    private val attachPaymentMethodInterceptorFactory: CustomerSheetAttachPaymentMethodInterceptor.Factory,
    private val logger: Logger,
) : IntentConfirmationInterceptor {
    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        if (intent !is SetupIntent) {
            val cause = IllegalStateException("Cannot use payment intents in Customer Sheet!")

            return ConfirmationDefinition.Action.Fail(
                cause = cause,
                message = cause.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        }

        return runCatching {
            val paymentMethod = createPaymentMethod(confirmationOption.createParams)

            if (paymentMethod.isUnverifiedUSBankAccount()) {
                return ConfirmationDefinition.Action.Complete(
                    intent = intent.copy(paymentMethod = paymentMethod),
                    deferredIntentConfirmationType = null,
                    completedFullPaymentFlow = true,
                )
            }

            return when (integrationMetadata.attachmentStyle) {
                IntegrationMetadata.CustomerSheet.AttachmentStyle.SetupIntent -> {
                    val setupIntentInterceptor = setupIntentInterceptorFactory.create(clientAttributionMetadata)

                    setupIntentInterceptor.intercept(
                        intent = intent,
                        confirmationOption = PaymentMethodConfirmationOption.Saved(
                            paymentMethod = paymentMethod,
                            optionsParams = null,
                        ),
                        shippingValues = null,
                    )
                }
                IntegrationMetadata.CustomerSheet.AttachmentStyle.CreateAttach -> {
                    val attachPaymentMethodInterceptor = attachPaymentMethodInterceptorFactory.create()

                    attachPaymentMethodInterceptor.intercept(
                        intent = intent,
                        paymentMethod = paymentMethod,
                    )
                }
            }
        }.fold(
            onSuccess = { it },
            onFailure = { cause ->
                ConfirmationDefinition.Action.Fail(
                    cause = cause,
                    message = cause.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        val error = IllegalStateException(
            "Cannot use CustomerSheetConfirmationInterceptor with saved payment methods!"
        )

        return ConfirmationDefinition.Action.Fail(
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            cause = error,
            message = error.stripeErrorMessage()
        )
    }

    private suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
    ): PaymentMethod {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = paymentMethodCreateParams,
            options = requestOptions,
        ).onFailure { throwable ->
            logger.error(
                msg = "Failed to create payment method for ${paymentMethodCreateParams.typeCode}",
                t = throwable,
            )
        }.getOrThrow()
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

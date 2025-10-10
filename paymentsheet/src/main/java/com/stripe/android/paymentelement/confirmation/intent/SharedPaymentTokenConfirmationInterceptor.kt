package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.utils.updatedWithProductUsage
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class SharedPaymentTokenConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val intentConfiguration: PaymentSheet.IntentConfiguration,
    @Assisted private val handler: PreparePaymentMethodHandler,
    private val errorReporter: ErrorReporter,
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.createPaymentMethod(
            confirmationOption.createParams.updatedWithProductUsage(intentConfiguration),
            requestOptions
        ).fold(
            onSuccess = { paymentMethod ->
                intercept(
                    intent = intent,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
                )
            },
            onFailure = { error ->
                ConfirmationDefinition.Action.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        ephemeralKeySecret: String?,
    ): ConfirmationDefinition.Action<Args> {
        return intercept(
            intent = intent,
            paymentMethod = confirmationOption.paymentMethod,
            shippingValues = shippingValues,
        )
    }

    private suspend fun intercept(
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        runCatching {
            stripeRepository.createSavedPaymentMethodRadarSession(
                paymentMethodId = paymentMethod.id
                    ?: throw GenericStripeException(
                        cause = IllegalStateException(
                            "No payment method ID was found for provided 'PaymentMethod' object!"
                        ),
                        analyticsValue = "noPaymentMethodId"
                    ),
                requestOptions = requestOptions,
            ).getOrThrow()
        }.onFailure {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.SAVED_PAYMENT_METHOD_RADAR_SESSION_FAILURE,
                stripeException = StripeException.create(it),
            )
        }

        return try {
            handler.onPreparePaymentMethod(
                paymentMethod = paymentMethod,
                shippingAddress = shippingValues?.toAddressDetails(),
            )

            ConfirmationDefinition.Action.Complete(
                intent = intent,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                completedFullPaymentFlow = false,
            )
        } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
            ConfirmationDefinition.Action.Fail(
                cause = exception,
                message = exception.errorMessage,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
    }

    private fun ConfirmPaymentIntentParams.Shipping.toAddressDetails(): AddressDetails {
        return AddressDetails(
            name = getName(),
            phoneNumber = getPhone(),
            address = getAddress().run {
                PaymentSheet.Address(
                    line1 = line1,
                    line2 = line2,
                    city = city,
                    country = country,
                    postalCode = postalCode,
                    state = state,
                )
            }
        )
    }

    @AssistedFactory
    interface Factory {
        @OptIn(SharedPaymentTokenSessionPreview::class)
        fun create(
            intentConfiguration: PaymentSheet.IntentConfiguration,
            handler: PreparePaymentMethodHandler
        ): SharedPaymentTokenConfirmationInterceptor
    }
}

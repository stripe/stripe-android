package com.stripe.android.paymentsheet

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import dagger.Lazy
import javax.inject.Inject

internal interface DeferredIntentRepository {

    sealed interface Result {
        data class Error(val error: String) : Result
        data class Success(
            val clientSecret: ClientSecret,
            val isConfirmed: Boolean
        ) : Result
    }
    suspend fun get(
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode,
        confirmCallback: ConfirmCallback?
    ): Result
}

internal class DefaultDeferredIntentRepository @Inject constructor(
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val stripeRepository: StripeRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val stripeIntentValidator: StripeIntentValidator
) : DeferredIntentRepository {

    override suspend fun get(
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode,
        confirmCallback: ConfirmCallback?
    ): DeferredIntentRepository.Result {
        val paymentMethodId = retrievePaymentMethodId(paymentSelection)
        val (confirmResponse, serverSideConfirmed) = retrieveConfirmResponseForDeferredIntent(
            confirmCallback = confirmCallback,
            paymentMethodId = paymentMethodId,
            shouldSavePaymentMethod = (paymentSelection as? PaymentSelection.New)
                ?.customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val clientSecret = when (confirmResponse) {
            is ConfirmCallback.Result.Failure -> {
                return DeferredIntentRepository.Result.Error(confirmResponse.error)
            }
            is ConfirmCallback.Result.Success -> {
                if (initializationMode.isProcessingPayment) {
                    PaymentIntentClientSecret(confirmResponse.clientSecret)
                } else {
                    SetupIntentClientSecret(confirmResponse.clientSecret)
                }
            }
        }

        return if (serverSideConfirmed) {
            val stripeIntent = retrieveDeferredIntent(initializationMode, clientSecret)
            val isConfirmed = stripeIntentValidator.isConfirmed(stripeIntent)
            if (isConfirmed) {
                DeferredIntentRepository.Result.Success(
                    clientSecret = clientSecret,
                    isConfirmed = true
                )
            } else {
                DeferredIntentRepository.Result.Success(
                    clientSecret = clientSecret,
                    isConfirmed = false
                )
            }
        } else {
            DeferredIntentRepository.Result.Success(
                clientSecret = clientSecret,
                isConfirmed = false
            )
        }
    }

    private suspend fun retrieveDeferredIntent(
        mode: PaymentSheet.InitializationMode,
        clientSecret: ClientSecret
    ): StripeIntent {
        return elementsSessionRepository.get(
            if (mode.isProcessingPayment) {
                PaymentSheet.InitializationMode.PaymentIntent(clientSecret.value)
            } else {
                PaymentSheet.InitializationMode.SetupIntent(clientSecret.value)
            }
        ).stripeIntent
    }

    private suspend fun retrievePaymentMethodId(
        paymentSelection: PaymentSelection?
    ): String {
        return paymentSelection?.let {
            when (paymentSelection) {
                is PaymentSelection.Saved -> {
                    paymentSelection.paymentMethod.id
                }
                is PaymentSelection.New -> {
                    stripeRepository.createPaymentMethod(
                        paymentSelection.paymentMethodCreateParams,
                        ApiRequest.Options(
                            apiKey = lazyPaymentConfig.get().publishableKey,
                            stripeAccount = lazyPaymentConfig.get().stripeAccountId
                        )
                    )?.id
                }
                else -> {
                    null
                }
            }
        } ?: error(
            "A valid payment method ID is required for the DeferredIntent flow"
        )
    }

    private suspend fun retrieveConfirmResponseForDeferredIntent(
        confirmCallback: ConfirmCallback?,
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean
    ): Pair<ConfirmCallback.Result, Boolean> {
        return when (confirmCallback) {
            is ConfirmCallbackForServerSideConfirmation -> {
                Pair(
                    confirmCallback.onConfirmResponse(
                        paymentMethodId,
                        shouldSavePaymentMethod
                    ),
                    true
                )
            }
            is ConfirmCallbackForClientSideConfirmation -> {
                Pair(
                    confirmCallback.onConfirmResponse(
                        paymentMethodId
                    ),
                    false
                )
            }
            else -> error(
                "ConfirmCallback must be implemented for DeferredIntent flow"
            )
        }
    }
}

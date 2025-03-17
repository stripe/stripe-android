package com.stripe.android.paymentelement.confirmation.lpms.foundations

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.StripeNetworkTestClient
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory

internal class CreateIntentFactory(
    private val paymentElementCallbackIdentifier: String,
    private val paymentMethodType: PaymentMethod.Type,
    private val testClient: StripeNetworkTestClient
) {
    suspend fun createPaymentIntent(
        country: MerchantCountry,
        amount: Int,
        currency: String,
        createWithSetupFutureUsage: Boolean
    ): Result<CreateIntentData> {
        return testClient.createPaymentIntent(
            country = country,
            amount = amount,
            currency = currency,
            paymentMethodType = paymentMethodType,
            createWithSetupFutureUsage = createWithSetupFutureUsage,
        ).mapCatching { clientSecret ->
            CreateIntentData(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = clientSecret,
                ),
                intent = testClient.retrievePaymentIntent(clientSecret).getOrThrow(),
            )
        }
    }

    fun createDeferredPaymentIntent(
        country: MerchantCountry,
        amount: Int,
        currency: String,
        createWithSetupFutureUsage: Boolean,
    ): Result<CreateIntentData> {
        PaymentElementCallbackReferences.set(
            key = paymentElementCallbackIdentifier,
            callbacks = PaymentElementCallbacks(
                createIntentCallback = { paymentMethod, _ ->
                    testClient.createPaymentIntent(
                        country = country,
                        amount = amount,
                        currency = currency,
                        paymentMethodType = paymentMethodType,
                        paymentMethodId = paymentMethod.id,
                        createWithSetupFutureUsage = createWithSetupFutureUsage,
                    ).fold(
                        onSuccess = {
                            CreateIntentResult.Success(it)
                        },
                        onFailure = { exception ->
                            CreateIntentResult.Failure(
                                cause = Exception(exception),
                                displayMessage = exception.message,
                            )
                        }
                    )
                },
                externalPaymentMethodConfirmHandler = null,
            )
        )

        return Result.success(
            CreateIntentData(
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = amount.toLong(),
                            currency = currency,
                            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession.takeIf {
                                createWithSetupFutureUsage
                            },
                        )
                    )
                ),
                // This intent is never used in the deferred mode so it's safe to make a mocked one here
                intent = PaymentIntentFactory.create(),
            )
        )
    }

    suspend fun createSetupIntent(
        country: MerchantCountry,
    ): Result<CreateIntentData> {
        return testClient.createSetupIntent(
            country = country,
            paymentMethodType = paymentMethodType,
        ).mapCatching { clientSecret ->
            CreateIntentData(
                initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                    clientSecret = clientSecret,
                ),
                intent = testClient.retrieveSetupIntent(clientSecret).getOrThrow(),
            )
        }
    }

    fun createDeferredSetupIntent(
        country: MerchantCountry,
    ): Result<CreateIntentData> {
        PaymentElementCallbackReferences.set(
            key = paymentElementCallbackIdentifier,
            callbacks = PaymentElementCallbacks(
                createIntentCallback = { paymentMethod, _ ->
                    testClient.createSetupIntent(
                        country = country,
                        paymentMethodType = paymentMethodType,
                        paymentMethodId = paymentMethod.id,
                    ).fold(
                        onSuccess = {
                            CreateIntentResult.Success(it)
                        },
                        onFailure = { exception ->
                            CreateIntentResult.Failure(
                                cause = Exception(exception),
                                displayMessage = exception.message,
                            )
                        }
                    )
                },
                externalPaymentMethodConfirmHandler = null,
            )
        )

        return Result.success(
            CreateIntentData(
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                        )
                    )
                ),
                intent = SetupIntentFactory.create(),
            )
        )
    }

    data class CreateIntentData(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val intent: StripeIntent,
    )
}

package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentIntentValidator
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultFlowControllerInitializer(
    private val stripeRepository: StripeRepository,
    private val paymentSessionPrefs: PaymentSessionPrefs,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val workContext: CoroutineContext
) : FlowControllerInitializer {
    private val paymentIntentValidator = PaymentIntentValidator()

    override suspend fun init(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration
    ) = withContext(workContext) {
        configuration.customer?.let { customerConfig ->
            createWithCustomer(
                paymentIntentClientSecret,
                customerConfig,
                configuration
            )
        } ?: createWithoutCustomer(paymentIntentClientSecret, configuration)
    }

    override suspend fun init(
        paymentIntentClientSecret: String
    ) = withContext(workContext) {
        createWithoutCustomer(
            paymentIntentClientSecret,
            config = null
        )
    }

    private suspend fun createWithCustomer(
        clientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration,
        config: PaymentSheet.Configuration?
    ): FlowControllerInitializer.InitResult {
        // load default payment option
        val defaultPaymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerConfig.id)

        return runCatching {
            retrievePaymentIntent(clientSecret)
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes.mapNotNull {
                    PaymentMethod.Type.fromCode(it)
                }
                retrieveAllPaymentMethods(
                    types = paymentMethodTypes,
                    customerConfig
                ).let { paymentMethods ->
                    FlowControllerInitializer.InitResult.Success(
                        InitData(
                            config = config,
                            paymentIntent = paymentIntent,
                            paymentMethodTypes = paymentMethodTypes,
                            paymentMethods = paymentMethods,
                            defaultPaymentMethodId = defaultPaymentMethodId
                        )
                    )
                }
            },
            onFailure = {
                FlowControllerInitializer.InitResult.Failure(it)
            }
        )
    }

    private suspend fun createWithoutCustomer(
        clientSecret: String,
        config: PaymentSheet.Configuration?
    ): FlowControllerInitializer.InitResult {
        return runCatching {
            retrievePaymentIntent(clientSecret)
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes
                    .mapNotNull {
                        PaymentMethod.Type.fromCode(it)
                    }

                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        config = config,
                        paymentIntent = paymentIntent,
                        paymentMethodTypes = paymentMethodTypes,
                        paymentMethods = emptyList(),
                        defaultPaymentMethodId = null
                    )
                )
            },
            onFailure = {
                FlowControllerInitializer.InitResult.Failure(it)
            }
        )
    }

    private suspend fun retrieveAllPaymentMethods(
        types: List<PaymentMethod.Type>,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        return types.flatMap { type ->
            retrievePaymentMethodsByType(type, customerConfig)
        }
    }

    /**
     * Return empty list on failure.
     */
    private suspend fun retrievePaymentMethodsByType(
        type: PaymentMethod.Type,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        return runCatching {
            stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerConfig.id,
                    paymentMethodType = type
                ),
                publishableKey,
                PRODUCT_USAGE,
                ApiRequest.Options(customerConfig.ephemeralKeySecret, stripeAccountId)
            )
        }.getOrDefault(emptyList())
    }

    private suspend fun retrievePaymentIntent(
        clientSecret: String
    ): PaymentIntent {
        return paymentIntentValidator.requireValid(
            requireNotNull(
                stripeRepository.retrievePaymentIntent(
                    clientSecret,
                    ApiRequest.Options(
                        publishableKey,
                        stripeAccountId
                    )
                )
            )
        )
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}

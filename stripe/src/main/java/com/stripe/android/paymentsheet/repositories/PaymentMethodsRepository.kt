package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Logger
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal sealed class PaymentMethodsRepository {
    abstract suspend fun get(
        customerConfig: PaymentSheet.CustomerConfiguration,
        type: PaymentMethod.Type
    ): List<PaymentMethod>

    abstract suspend fun save(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
    ): Any?

    /**
     * A [PaymentMethodsRepository] that uses a pre-determined list of payment methods.
     */
    class Static(
        private val paymentMethods: List<PaymentMethod>
    ) : PaymentMethodsRepository() {
        override suspend fun get(
            customerConfig: PaymentSheet.CustomerConfiguration,
            type: PaymentMethod.Type
        ): List<PaymentMethod> = paymentMethods

        override suspend fun save(
            customerConfig: PaymentSheet.CustomerConfiguration,
            paymentMethodCreateParams: PaymentMethodCreateParams
        ) {
            TODO("Not yet implemented")
        }
    }

    /**
     * A [PaymentMethodsRepository] that uses the Stripe API.
     */
    class Api(
        private val stripeRepository: StripeRepository,
        private val publishableKey: String,
        private val stripeAccountId: String?,
        enableLogging: Boolean = false,
        private val workContext: CoroutineContext
    ) : PaymentMethodsRepository() {
        private val logger = Logger.getInstance(enableLogging)

        /**
         * Retrieve a Customer's payment methods. Silently handle failures by returning an
         * empty list.
         */
        override suspend fun get(
            customerConfig: PaymentSheet.CustomerConfiguration,
            type: PaymentMethod.Type
        ): List<PaymentMethod> = withContext(workContext) {
            runCatching {
                stripeRepository.getPaymentMethods(
                    ListPaymentMethodsParams(
                        customerId = customerConfig.id,
                        paymentMethodType = type
                    ),
                    publishableKey,
                    PRODUCT_USAGE,
                    ApiRequest.Options(
                        customerConfig.ephemeralKeySecret,
                        stripeAccountId
                    )
                )
            }.onFailure {
                logger.error("Failed to retrieve ${customerConfig.id}'s payment methods.", it)
            }.getOrDefault(emptyList())
        }

        override suspend fun save(
            customerConfig: PaymentSheet.CustomerConfiguration,
            paymentMethodCreateParams: PaymentMethodCreateParams
        ) {
            withContext(workContext) {
                runCatching {
                    stripeRepository.createPaymentMethod(
                        paymentMethodCreateParams,
                        ApiRequest.Options(
                            customerConfig.ephemeralKeySecret,
                            stripeAccountId
                        )
                    )?.let { paymentMethod ->
                        paymentMethod.id?.let {
                            stripeRepository.attachPaymentMethod(
                                customerConfig.id,
                                publishableKey,
                                PRODUCT_USAGE,
                                paymentMethod.id,
                                ApiRequest.Options(
                                    customerConfig.ephemeralKeySecret,
                                    stripeAccountId
                                )
                            )
                        }
                    }
                }.onFailure {
                    logger.error("Failed to save ${customerConfig.id}'s payment methods.", it)
                    throw it
                }
            }
        }

        private companion object {
            private val PRODUCT_USAGE = setOf("PaymentSheet")
        }
    }
}

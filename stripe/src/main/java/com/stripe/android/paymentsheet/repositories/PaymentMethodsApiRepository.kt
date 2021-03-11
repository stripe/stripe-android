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

internal class PaymentMethodsApiRepository(
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    enableLogging: Boolean = false,
    private val workContext: CoroutineContext
) : PaymentMethodsRepository {
    /**
     * A [PaymentMethodsRepository] that uses the Stripe API.
     */
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
    ): PaymentMethod = withContext(workContext) {
        runCatching {
            val paymentMethod = requireNotNull(
                stripeRepository.createPaymentMethod(
                    paymentMethodCreateParams,
                    ApiRequest.Options(
                        publishableKey,
                        stripeAccountId
                    )
                )
            ) {
                ERROR_MSG
            }

            requireNotNull(paymentMethod.id) {
                ERROR_MSG
            }

            requireNotNull(
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
            ) {
                ERROR_MSG
            }
        }.onFailure {
            logger.error("Failed to save ${customerConfig.id}'s payment methods.", it)
            throw it
        }.getOrThrow()
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
        private val ERROR_MSG = "Could not parse PaymentMethod."
    }
}

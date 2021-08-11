package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Logger
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * A [CustomerRepository] that uses the Stripe API.
 */
internal class CustomerApiRepository(
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val logger: Logger,
    private val workContext: CoroutineContext
) : CustomerRepository {
    /**
     * Retrieve a Customer's payment methods of all types requested.
     * Silently handle failures by returning an empty list for the payment method types that failed.
     */
    override suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod> = withContext(workContext) {
        supervisorScope {
            types.map { paymentMethodType ->
                async {
                    stripeRepository.getPaymentMethods(
                        ListPaymentMethodsParams(
                            customerId = customerConfig.id,
                            paymentMethodType = paymentMethodType
                        ),
                        publishableKey,
                        PRODUCT_USAGE,
                        ApiRequest.Options(
                            customerConfig.ephemeralKeySecret,
                            stripeAccountId
                        )
                    )
                }
            }.map {
                runCatching {
                    it.await()
                }.onFailure {
                    logger.error("Failed to retrieve payment methods.", it)
                }.getOrDefault(emptyList())
            }.flatten()
        }
    }

    /**
     * Detach a payment method from the Customer and return the modified [PaymentMethod].
     * Silently handle failures by returning null.
     */
    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod? =
        withContext(workContext) {
            runCatching {
                stripeRepository.detachPaymentMethod(
                    publishableKey,
                    PRODUCT_USAGE,
                    paymentMethodId,
                    ApiRequest.Options(
                        customerConfig.ephemeralKeySecret,
                        stripeAccountId
                    )
                )
            }.onFailure {
                logger.error("Failed to detach payment method ${paymentMethodId}.", it)
            }.getOrNull()
        }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}

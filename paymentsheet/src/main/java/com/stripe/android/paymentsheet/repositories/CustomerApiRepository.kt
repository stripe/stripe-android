package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * A [CustomerRepository] that uses the Stripe API.
 */
@Singleton
internal class CustomerApiRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext,
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String> = emptySet()
) : CustomerRepository {

    override suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String
    ): Customer? {
        return stripeRepository.retrieveCustomer(
            customerId,
            productUsageTokens,
            ApiRequest.Options(
                ephemeralKeySecret,
                lazyPaymentConfig.get().stripeAccountId
            )
        ).getOrNull()
    }

    override suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod> = withContext(workContext) {
        val requests = types.map { paymentMethodType ->
            async {
                stripeRepository.getPaymentMethods(
                    listPaymentMethodsParams = ListPaymentMethodsParams(
                        customerId = customerConfig.id,
                        paymentMethodType = paymentMethodType,
                    ),
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    productUsageTokens = productUsageTokens,
                    requestOptions = ApiRequest.Options(
                        apiKey = customerConfig.ephemeralKeySecret,
                        stripeAccount = lazyPaymentConfig.get().stripeAccountId,
                    ),
                ).onFailure {
                    logger.error("Failed to retrieve payment methods.", it)
                }
            }
        }

        requests.awaitAll().flatMap {
            it.getOrElse { emptyList() }
        }
    }

    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod? =
        withContext(workContext) {
            stripeRepository.detachPaymentMethod(
                publishableKey = lazyPaymentConfig.get().publishableKey,
                productUsageTokens = productUsageTokens,
                paymentMethodId = paymentMethodId,
                requestOptions = ApiRequest.Options(
                    apiKey = customerConfig.ephemeralKeySecret,
                    stripeAccount = lazyPaymentConfig.get().stripeAccountId,
                )
            ).onFailure {
                logger.error("Failed to detach payment method $paymentMethodId.", it)
            }.getOrNull()
        }
}

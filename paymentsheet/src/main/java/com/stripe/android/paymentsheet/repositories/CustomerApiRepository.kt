package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.PaymentSheet
import dagger.Lazy
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * A [CustomerRepository] that uses the Stripe API.
 */
@Singleton
internal class CustomerApiRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext,
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String> = emptySet()
) : CustomerRepository {

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
                        lazyPaymentConfig.get().publishableKey,
                        productUsageTokens,
                        ApiRequest.Options(
                            customerConfig.ephemeralKeySecret,
                            lazyPaymentConfig.get().stripeAccountId
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

    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod? =
        withContext(workContext) {
            runCatching {
                stripeRepository.detachPaymentMethod(
                    lazyPaymentConfig.get().publishableKey,
                    productUsageTokens,
                    paymentMethodId,
                    ApiRequest.Options(
                        customerConfig.ephemeralKeySecret,
                        lazyPaymentConfig.get().stripeAccountId
                    )
                )
            }.onFailure {
                logger.error("Failed to detach payment method $paymentMethodId.", it)
            }.getOrNull()
        }
}

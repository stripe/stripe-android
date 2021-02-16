package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
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

    /**
     * Retrieve the [PaymentMethod] list from a static source.
     */
    class Static(
        private val paymentMethods: List<PaymentMethod>
    ) : PaymentMethodsRepository() {
        override suspend fun get(
            customerConfig: PaymentSheet.CustomerConfiguration,
            type: PaymentMethod.Type
        ): List<PaymentMethod> = paymentMethods
    }

    /**
     * Retrieve the [PaymentMethod] list from the API.
     */
    class Api(
        private val stripeRepository: StripeRepository,
        private val publishableKey: String,
        private val stripeAccountId: String?,
        private val workContext: CoroutineContext
    ) : PaymentMethodsRepository() {
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
            }.getOrDefault(emptyList())
        }

        private companion object {
            private val PRODUCT_USAGE = setOf("PaymentSheet")
        }
    }
}

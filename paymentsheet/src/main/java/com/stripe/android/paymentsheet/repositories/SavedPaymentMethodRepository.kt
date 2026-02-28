package com.stripe.android.paymentsheet.repositories

import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import javax.inject.Inject

/**
 * Repository for managing saved payment methods. This abstracts over the underlying
 * implementation (e.g. CustomerRepository for legacy/customer-session flows, or
 * CheckoutSessionRepository for checkout session flows).
 */
internal interface SavedPaymentMethodRepository {
    suspend fun detachPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod>

    suspend fun setDefaultPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String?,
    ): Result<Customer>
}

internal class DefaultSavedPaymentMethodRepository @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val checkoutSessionRepository: CheckoutSessionRepository,
) : SavedPaymentMethodRepository {

    private fun CustomerMetadata.toCustomerInfo() = CustomerRepository.CustomerInfo(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        customerSessionClientSecret = customerSessionClientSecret,
    )

    override suspend fun detachPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod> {
        val checkoutSessionId = customerMetadata.checkoutSessionId
        if (checkoutSessionId != null) {
            return checkoutSessionRepository.detachPaymentMethod(
                sessionId = checkoutSessionId,
                paymentMethodId = paymentMethodId,
            ).map {
                PaymentMethod.Builder().setId(paymentMethodId).build()
            }
        }

        return customerRepository.detachPaymentMethod(
            customerInfo = customerMetadata.toCustomerInfo(),
            paymentMethodId = paymentMethodId,
            canRemoveDuplicates = canRemoveDuplicates,
        )
    }

    override suspend fun updatePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> = customerRepository.updatePaymentMethod(
        customerInfo = customerMetadata.toCustomerInfo(),
        paymentMethodId = paymentMethodId,
        params = params,
    )

    override suspend fun setDefaultPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String?,
    ): Result<Customer> = customerRepository.setDefaultPaymentMethod(
        customerInfo = customerMetadata.toCustomerInfo(),
        paymentMethodId = paymentMethodId,
    )
}

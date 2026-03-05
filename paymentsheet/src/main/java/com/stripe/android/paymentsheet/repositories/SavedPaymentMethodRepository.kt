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

    override suspend fun detachPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            checkoutSessionRepository.detachPaymentMethod(
                sessionId = customerMetadata.sessionId,
                paymentMethodId = paymentMethodId,
            ).map {
                PaymentMethod.Builder().setId(paymentMethodId).build()
            }
        }
        is CustomerMetadata.Session -> {
            customerRepository.detachPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                customerSessionClientSecret = customerMetadata.customerSessionClientSecret,
                paymentMethodId = paymentMethodId,
                canRemoveDuplicates = canRemoveDuplicates,
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.detachPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                canRemoveDuplicates = canRemoveDuplicates,
            )
        }
    }

    override suspend fun updatePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            Result.failure(NotImplementedError("Checkout sessions do not support updating payment methods"))
        }
        is CustomerMetadata.Session -> {
            customerRepository.updatePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                params = params,
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.updatePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                params = params,
            )
        }
    }

    override suspend fun setDefaultPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String?,
    ): Result<Customer> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            Result.failure(NotImplementedError("Checkout sessions do not support setting default payment methods"))
        }
        is CustomerMetadata.Session -> {
            customerRepository.setDefaultPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.setDefaultPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
            )
        }
    }
}

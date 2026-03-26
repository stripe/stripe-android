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

    suspend fun retrievePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
    ): Result<PaymentMethod>
}

internal class DefaultSavedPaymentMethodRepository @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val checkoutSessionRepository: CheckoutSessionRepository,
) : SavedPaymentMethodRepository {

    override suspend fun detachPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            checkoutSessionRepository.detachPaymentMethod(
                sessionId = customerMetadata.sessionId,
                paymentMethodId = paymentMethodId,
            ).map {
                PaymentMethod.Builder().setId(paymentMethodId).build()
            }
        }
        is CustomerMetadata.CustomerSession -> {
            customerRepository.detachPaymentMethodAndDuplicates(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                customerSessionClientSecret = customerMetadata.customerSessionClientSecret,
                paymentMethodId = paymentMethodId,
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.detachPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
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
        is CustomerMetadata.CustomerSession -> {
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
        is CustomerMetadata.CustomerSession -> {
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

    override suspend fun retrievePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            Result.failure(
                NotImplementedError("Checkout sessions do not support retrieving individual payment methods")
            )
        }
        is CustomerMetadata.CustomerSession -> {
            customerRepository.retrievePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.retrievePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
            )
        }
    }
}

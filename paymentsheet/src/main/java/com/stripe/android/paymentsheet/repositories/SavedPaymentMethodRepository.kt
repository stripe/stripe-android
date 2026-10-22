package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Provider

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
    private val customerRepository: Lazy<CustomerRepository>,
    private val checkoutSessionRepository: Lazy<CheckoutSessionRepository>,
    private val elementsSessionClientParams: ElementsSessionClientParams,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
) : SavedPaymentMethodRepository {

    override suspend fun detachPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            checkoutSessionRepository.get().detachPaymentMethod(
                sessionId = customerMetadata.sessionId,
                paymentMethodId = paymentMethodId,
                clientParams = elementsSessionClientParams,
            ).map {
                PaymentMethod.Builder().setId(paymentMethodId).build()
            }
        }
        is CustomerMetadata.CustomerSession -> {
            customerRepository.get().detachPaymentMethodAndDuplicates(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                customerSessionClientSecret = customerMetadata.customerSessionClientSecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.get().detachPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
    }

    override suspend fun updatePaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> = when (customerMetadata) {
        is CustomerMetadata.CheckoutSession -> {
            checkoutSessionRepository.get().updatePaymentMethod(
                sessionId = customerMetadata.sessionId,
                paymentMethodId = paymentMethodId,
                params = params,
            ).mapCatching { response ->
                response.customer?.paymentMethods?.firstOrNull { it.id == paymentMethodId }
                    ?: error("Checkout session update response did not include updated payment method.")
            }
        }
        is CustomerMetadata.CustomerSession -> {
            customerRepository.get().updatePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                params = params,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.get().updatePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
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
            customerRepository.get().setDefaultPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.get().setDefaultPaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
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
            customerRepository.get().retrievePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
        is CustomerMetadata.LegacyEphemeralKey -> {
            customerRepository.get().retrievePaymentMethod(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfigurationProvider.get(),
            )
        }
    }
}

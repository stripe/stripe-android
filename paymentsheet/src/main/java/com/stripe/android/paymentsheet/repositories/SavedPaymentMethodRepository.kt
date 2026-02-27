package com.stripe.android.paymentsheet.repositories

import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import javax.inject.Inject

/**
 * Repository that abstracts saved payment method management operations (detach, update, set default).
 *
 * This sits between [SavedPaymentMethodMutator] and the underlying data sources
 * ([CustomerRepository] and [CheckoutSessionRepository]), following the Android architecture
 * data layer pattern for multiple levels of repositories.
 *
 * @see <a href="https://developer.android.com/topic/architecture/data-layer#multiple-levels">Multiple levels of repositories</a>
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
    ): Result<PaymentMethod> {
        val checkoutSessionId = customerMetadata.checkoutSessionId
        if (checkoutSessionId != null) {
            return checkoutSessionRepository.detachPaymentMethod(
                sessionId = checkoutSessionId,
                paymentMethodId = paymentMethodId,
            ).map {
                // The checkout session API returns the full session response, but the caller
                // needs the detached PaymentMethod. Build a minimal PM with the ID.
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
    ): Result<PaymentMethod> {
        return customerRepository.updatePaymentMethod(
            customerInfo = customerMetadata.toCustomerInfo(),
            paymentMethodId = paymentMethodId,
            params = params,
        )
    }

    override suspend fun setDefaultPaymentMethod(
        customerMetadata: CustomerMetadata,
        paymentMethodId: String?,
    ): Result<Customer> {
        return customerRepository.setDefaultPaymentMethod(
            customerInfo = customerMetadata.toCustomerInfo(),
            paymentMethodId = paymentMethodId,
        )
    }

    private fun CustomerMetadata.toCustomerInfo(): CustomerRepository.CustomerInfo {
        return CustomerRepository.CustomerInfo(
            id = id,
            ephemeralKeySecret = ephemeralKeySecret,
            customerSessionClientSecret = customerSessionClientSecret,
        )
    }
}

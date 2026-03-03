package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import javax.inject.Inject

/**
 * Carries the routing context that [DefaultSavedPaymentMethodRepository] needs to decide
 * which backend to delegate to.
 */
internal sealed interface SavedPaymentMethodAccess {
    data class Customer(
        val info: CustomerRepository.CustomerInfo,
    ) : SavedPaymentMethodAccess

    data class CheckoutSession(
        val sessionId: String,
    ) : SavedPaymentMethodAccess
}

/**
 * Repository for managing saved payment methods. This abstracts over the underlying
 * implementation (e.g. CustomerRepository for legacy/customer-session flows, or
 * CheckoutSessionRepository for checkout session flows).
 */
internal interface SavedPaymentMethodRepository {
    suspend fun detachPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod>

    suspend fun setDefaultPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String?,
    ): Result<Customer>
}

internal class DefaultSavedPaymentMethodRepository @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val checkoutSessionRepository: CheckoutSessionRepository,
) : SavedPaymentMethodRepository {

    override suspend fun detachPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod> = when (access) {
        is SavedPaymentMethodAccess.CheckoutSession -> {
            checkoutSessionRepository.detachPaymentMethod(
                sessionId = access.sessionId,
                paymentMethodId = paymentMethodId,
            ).map {
                PaymentMethod.Builder().setId(paymentMethodId).build()
            }
        }
        is SavedPaymentMethodAccess.Customer -> {
            customerRepository.detachPaymentMethod(
                customerInfo = access.info,
                paymentMethodId = paymentMethodId,
                canRemoveDuplicates = canRemoveDuplicates,
            )
        }
    }

    override suspend fun updatePaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> = when (access) {
        is SavedPaymentMethodAccess.CheckoutSession -> {
            Result.failure(NotImplementedError("Checkout sessions do not support updating payment methods"))
        }
        is SavedPaymentMethodAccess.Customer -> {
            customerRepository.updatePaymentMethod(
                customerInfo = access.info,
                paymentMethodId = paymentMethodId,
                params = params,
            )
        }
    }

    override suspend fun setDefaultPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String?,
    ): Result<Customer> = when (access) {
        is SavedPaymentMethodAccess.CheckoutSession -> {
            Result.failure(NotImplementedError("Checkout sessions do not support setting default payment methods"))
        }
        is SavedPaymentMethodAccess.Customer -> {
            customerRepository.setDefaultPaymentMethod(
                customerInfo = access.info,
                paymentMethodId = paymentMethodId,
            )
        }
    }
}

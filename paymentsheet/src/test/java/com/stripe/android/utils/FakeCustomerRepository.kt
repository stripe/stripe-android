package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.repositories.CustomerRepository

internal open class FakeCustomerRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList(),
    private val customer: Customer? = null,
    private val onRetrieveCustomer: () -> Customer? = {
        customer
    },
    private val onGetPaymentMethods: () -> Result<List<PaymentMethod>> = {
        Result.success(paymentMethods)
    },
    private val onDetachPaymentMethod: (paymentMethodId: String) -> Result<PaymentMethod> = { paymentMethodId ->
        paymentMethods.find { it.id == paymentMethodId }?.let {
            Result.success(it)
        } ?: Result.failure(IllegalArgumentException("Could not find payment method to remove"))
    },
    private val onAttachPaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onUpdatePaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onSetDefaultPaymentMethod: () -> Result<Customer> = {
        Result.failure(NotImplementedError())
    }
) : CustomerRepository {
    private val _detachRequests = Turbine<DetachRequest>()
    val detachRequests: ReceiveTurbine<DetachRequest> = _detachRequests

    private val _updateRequests = Turbine<UpdateRequest>()
    val updateRequests: ReceiveTurbine<UpdateRequest> = _updateRequests

    private val _setDefaultPaymentMethodRequests = Turbine<SetDefaultRequest>()
    val setDefaultPaymentMethodRequests: ReceiveTurbine<SetDefaultRequest> = _setDefaultPaymentMethodRequests

    var error: Throwable? = null

    override suspend fun retrieveCustomer(
        accessInfo: CustomerMetadata.AccessInfo
    ): Customer? = onRetrieveCustomer()

    override suspend fun getPaymentMethods(
        accessInfo: CustomerMetadata.AccessInfo,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>> = onGetPaymentMethods()

    override suspend fun detachPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                accessInfo = accessInfo,
                canRemoveDuplicates = canRemoveDuplicates,
            )
        )

        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun attachPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String
    ): Result<PaymentMethod> = onAttachPaymentMethod()

    override suspend fun updatePaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod> {
        _updateRequests.add(
            UpdateRequest(
                paymentMethodId = paymentMethodId,
                accessInfo = accessInfo,
                params = params,
            )
        )

        return onUpdatePaymentMethod()
    }

    override suspend fun setDefaultPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String?
    ): Result<Customer> {
        _setDefaultPaymentMethodRequests.add(
            SetDefaultRequest(paymentMethodId = paymentMethodId, accessInfo = accessInfo)
        )

        return onSetDefaultPaymentMethod()
    }

    data class DetachRequest(
        val paymentMethodId: String,
        val accessInfo: CustomerMetadata.AccessInfo,
        val canRemoveDuplicates: Boolean,
    )

    data class UpdateRequest(
        val paymentMethodId: String,
        val accessInfo: CustomerMetadata.AccessInfo,
        val params: PaymentMethodUpdateParams,
    )

    data class SetDefaultRequest(
        val paymentMethodId: String?,
        val accessInfo: CustomerMetadata.AccessInfo,
    )
}

package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
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
    },
    private val onRetrievePaymentMethod: (paymentMethodId: String) -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
) : CustomerRepository {
    private val _retrieveCustomerRequests = Turbine<RetrieveCustomerRequest>()
    val retrieveCustomerRequests: ReceiveTurbine<RetrieveCustomerRequest> = _retrieveCustomerRequests

    private val _getPaymentMethodsRequests = Turbine<GetPaymentMethodsRequest>()
    val getPaymentMethodsRequests: ReceiveTurbine<GetPaymentMethodsRequest> = _getPaymentMethodsRequests

    private val _detachRequests = Turbine<DetachRequest>()
    val detachRequests: ReceiveTurbine<DetachRequest> = _detachRequests

    private val _updateRequests = Turbine<UpdateRequest>()
    val updateRequests: ReceiveTurbine<UpdateRequest> = _updateRequests

    private val _setDefaultPaymentMethodRequests = Turbine<SetDefaultRequest>()
    val setDefaultPaymentMethodRequests: ReceiveTurbine<SetDefaultRequest> = _setDefaultPaymentMethodRequests

    open fun ensureAllEventsConsumed() {
        _retrieveCustomerRequests.ensureAllEventsConsumed()
        _getPaymentMethodsRequests.ensureAllEventsConsumed()
        _detachRequests.ensureAllEventsConsumed()
        _updateRequests.ensureAllEventsConsumed()
        _setDefaultPaymentMethodRequests.ensureAllEventsConsumed()
    }

    override suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String,
    ): Customer? {
        _retrieveCustomerRequests.add(
            RetrieveCustomerRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
            )
        )

        return onRetrieveCustomer()
    }

    override suspend fun getPaymentMethods(
        customerId: String,
        ephemeralKeySecret: String,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>> {
        _getPaymentMethodsRequests.add(
            GetPaymentMethodsRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                types = types,
                silentlyFail = silentlyFail,
            )
        )

        return onGetPaymentMethods()
    }

    override suspend fun detachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
            )
        )

        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun detachPaymentMethodAndDuplicates(
        customerId: String,
        ephemeralKeySecret: String,
        customerSessionClientSecret: String,
        paymentMethodId: String,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                customerSessionClientSecret = customerSessionClientSecret,
            )
        )

        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun attachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
    ): Result<PaymentMethod> = onAttachPaymentMethod()

    override suspend fun updatePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> {
        _updateRequests.add(
            UpdateRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                params = params,
            )
        )

        return onUpdatePaymentMethod()
    }

    override suspend fun setDefaultPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String?,
    ): Result<Customer> {
        _setDefaultPaymentMethodRequests.add(
            SetDefaultRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
            )
        )

        return onSetDefaultPaymentMethod()
    }

    override suspend fun retrievePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
    ): Result<PaymentMethod> = onRetrievePaymentMethod(paymentMethodId)

    data class RetrieveCustomerRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
    )

    data class GetPaymentMethodsRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
        val types: List<PaymentMethod.Type>,
        val silentlyFail: Boolean,
    )

    data class DetachRequest(
        val paymentMethodId: String,
        val customerId: String,
        val ephemeralKeySecret: String,
        val customerSessionClientSecret: String? = null,
    )

    data class UpdateRequest(
        val paymentMethodId: String,
        val customerId: String,
        val ephemeralKeySecret: String,
        val params: PaymentMethodUpdateParams,
    )

    data class SetDefaultRequest(
        val paymentMethodId: String?,
        val customerId: String,
        val ephemeralKeySecret: String,
    )
}

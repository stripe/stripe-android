package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.ApiConfiguration
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

    private val _attachRequests = Turbine<AttachRequest>()
    val attachRequests: ReceiveTurbine<AttachRequest> = _attachRequests

    private val _updateRequests = Turbine<UpdateRequest>()
    val updateRequests: ReceiveTurbine<UpdateRequest> = _updateRequests

    private val _setDefaultPaymentMethodRequests = Turbine<SetDefaultRequest>()
    val setDefaultPaymentMethodRequests: ReceiveTurbine<SetDefaultRequest> = _setDefaultPaymentMethodRequests

    private val _retrievePaymentMethodRequests = Turbine<RetrievePaymentMethodRequest>()
    val retrievePaymentMethodRequests: ReceiveTurbine<RetrievePaymentMethodRequest> = _retrievePaymentMethodRequests

    open fun ensureAllEventsConsumed() {
        _retrieveCustomerRequests.ensureAllEventsConsumed()
        _getPaymentMethodsRequests.ensureAllEventsConsumed()
        _detachRequests.ensureAllEventsConsumed()
        _attachRequests.ensureAllEventsConsumed()
        _updateRequests.ensureAllEventsConsumed()
        _setDefaultPaymentMethodRequests.ensureAllEventsConsumed()
        _retrievePaymentMethodRequests.ensureAllEventsConsumed()
    }

    override suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String,
        apiConfiguration: ApiConfiguration.State,
    ): Customer? {
        _retrieveCustomerRequests.add(
            RetrieveCustomerRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                apiConfiguration = apiConfiguration,
            )
        )

        return onRetrieveCustomer()
    }

    override suspend fun getPaymentMethods(
        customerId: String,
        ephemeralKeySecret: String,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
        apiConfiguration: ApiConfiguration.State,
    ): Result<List<PaymentMethod>> {
        _getPaymentMethodsRequests.add(
            GetPaymentMethodsRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                types = types,
                silentlyFail = silentlyFail,
                apiConfiguration = apiConfiguration,
            )
        )

        return onGetPaymentMethods()
    }

    override suspend fun detachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                apiConfiguration = apiConfiguration,
            )
        )

        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun detachPaymentMethodAndDuplicates(
        customerId: String,
        ephemeralKeySecret: String,
        customerSessionClientSecret: String,
        paymentMethodId: String,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                customerSessionClientSecret = customerSessionClientSecret,
                apiConfiguration = apiConfiguration,
            )
        )

        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun attachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        _attachRequests.add(
            AttachRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfiguration,
            )
        )

        return onAttachPaymentMethod()
    }

    override suspend fun updatePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        _updateRequests.add(
            UpdateRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                params = params,
                apiConfiguration = apiConfiguration,
            )
        )

        return onUpdatePaymentMethod()
    }

    override suspend fun setDefaultPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String?,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Customer> {
        _setDefaultPaymentMethodRequests.add(
            SetDefaultRequest(
                paymentMethodId = paymentMethodId,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                apiConfiguration = apiConfiguration,
            )
        )

        return onSetDefaultPaymentMethod()
    }

    override suspend fun retrievePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        _retrievePaymentMethodRequests.add(
            RetrievePaymentMethodRequest(
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                paymentMethodId = paymentMethodId,
                apiConfiguration = apiConfiguration,
            )
        )

        return onRetrievePaymentMethod(paymentMethodId)
    }

    data class RetrieveCustomerRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class GetPaymentMethodsRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
        val types: List<PaymentMethod.Type>,
        val silentlyFail: Boolean,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class DetachRequest(
        val paymentMethodId: String,
        val customerId: String,
        val ephemeralKeySecret: String,
        val customerSessionClientSecret: String? = null,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class AttachRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
        val paymentMethodId: String,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class UpdateRequest(
        val paymentMethodId: String,
        val customerId: String,
        val ephemeralKeySecret: String,
        val params: PaymentMethodUpdateParams,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class SetDefaultRequest(
        val paymentMethodId: String?,
        val customerId: String,
        val ephemeralKeySecret: String,
        val apiConfiguration: ApiConfiguration.State,
    )

    data class RetrievePaymentMethodRequest(
        val customerId: String,
        val ephemeralKeySecret: String,
        val paymentMethodId: String,
        val apiConfiguration: ApiConfiguration.State,
    )
}

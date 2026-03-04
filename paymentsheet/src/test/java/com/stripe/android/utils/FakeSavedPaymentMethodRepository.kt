package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodAccess
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository

internal class FakeSavedPaymentMethodRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList(),
    private val onDetachPaymentMethod: (paymentMethodId: String) -> Result<PaymentMethod> = { paymentMethodId ->
        paymentMethods.find { it.id == paymentMethodId }?.let {
            Result.success(it)
        } ?: Result.failure(IllegalArgumentException("Could not find payment method to remove"))
    },
    private val onUpdatePaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onSetDefaultPaymentMethod: () -> Result<Customer> = {
        Result.failure(NotImplementedError())
    },
) : SavedPaymentMethodRepository {
    private val _detachRequests = Turbine<DetachRequest>()
    val detachRequests: ReceiveTurbine<DetachRequest> = _detachRequests

    private val _updateRequests = Turbine<UpdateRequest>()
    val updateRequests: ReceiveTurbine<UpdateRequest> = _updateRequests

    private val _setDefaultPaymentMethodRequests = Turbine<SetDefaultRequest>()
    val setDefaultPaymentMethodRequests: ReceiveTurbine<SetDefaultRequest> = _setDefaultPaymentMethodRequests

    override suspend fun detachPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod> {
        _detachRequests.add(
            DetachRequest(
                paymentMethodId = paymentMethodId,
                access = access,
                canRemoveDuplicates = canRemoveDuplicates,
            )
        )
        return onDetachPaymentMethod(paymentMethodId)
    }

    override suspend fun updatePaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> {
        _updateRequests.add(
            UpdateRequest(
                paymentMethodId = paymentMethodId,
                access = access,
                params = params,
            )
        )
        return onUpdatePaymentMethod()
    }

    override suspend fun setDefaultPaymentMethod(
        access: SavedPaymentMethodAccess,
        paymentMethodId: String?,
    ): Result<Customer> {
        _setDefaultPaymentMethodRequests.add(
            SetDefaultRequest(
                paymentMethodId = paymentMethodId,
                access = access,
            )
        )
        return onSetDefaultPaymentMethod()
    }

    fun validate() {
        _detachRequests.ensureAllEventsConsumed()
        _updateRequests.ensureAllEventsConsumed()
        _setDefaultPaymentMethodRequests.ensureAllEventsConsumed()
    }

    data class DetachRequest(
        val paymentMethodId: String,
        val access: SavedPaymentMethodAccess,
        val canRemoveDuplicates: Boolean,
    )

    data class UpdateRequest(
        val paymentMethodId: String,
        val access: SavedPaymentMethodAccess,
        val params: PaymentMethodUpdateParams,
    )

    data class SetDefaultRequest(
        val paymentMethodId: String?,
        val access: SavedPaymentMethodAccess,
    )
}

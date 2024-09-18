package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.map
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.model.SavedSelection
import javax.inject.Inject

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerAdapterDataSource @Inject constructor(
    private val customerAdapter: CustomerAdapter,
) : CustomerSheetSavedSelectionDataSource,
    CustomerSheetPaymentMethodDataSource,
    CustomerSheetIntentDataSource {
    override suspend fun canCreateSetupIntents(): CustomerSheetDataResult<Boolean> {
        return CustomerSheetDataResult.success(customerAdapter.canCreateSetupIntents)
    }

    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return customerAdapter.retrievePaymentMethods().toCustomerSheetDataResult()
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.updatePaymentMethod(paymentMethodId, params).toCustomerSheetDataResult()
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.attachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.detachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        return customerAdapter.retrieveSelectedPaymentOption().map { result ->
            result?.toSavedSelection()
        }.toCustomerSheetDataResult()
    }

    override suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit> {
        return customerAdapter.setSelectedPaymentOption(selection?.toPaymentOption()).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> {
        return customerAdapter.setupIntentClientSecretForCustomerAttach().toCustomerSheetDataResult()
    }
}

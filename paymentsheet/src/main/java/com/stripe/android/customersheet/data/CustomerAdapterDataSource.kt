package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.map
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerAdapterDataSource @Inject constructor(
    private val customerAdapter: CustomerAdapter,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetSavedSelectionDataSource,
    CustomerSheetPaymentMethodDataSource,
    CustomerSheetIntentDataSource {
    override suspend fun canCreateSetupIntents(): CustomerSheetDataResult<Boolean> = withContext(workContext) {
        CustomerSheetDataResult.success(customerAdapter.canCreateSetupIntents)
    }

    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> =
        withContext(workContext) {
            customerAdapter.retrievePaymentMethods().toCustomerSheetDataResult()
        }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): CustomerSheetDataResult<PaymentMethod> = withContext(workContext) {
        customerAdapter.updatePaymentMethod(paymentMethodId, params).toCustomerSheetDataResult()
    }

    override suspend fun attachPaymentMethod(
        paymentMethodId: String
    ): CustomerSheetDataResult<PaymentMethod> = withContext(workContext) {
        customerAdapter.attachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun detachPaymentMethod(
        paymentMethodId: String
    ): CustomerSheetDataResult<PaymentMethod> = withContext(workContext) {
        customerAdapter.detachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> =
        withContext(workContext) {
            customerAdapter.retrieveSelectedPaymentOption().map { result ->
                result?.toSavedSelection()
            }.toCustomerSheetDataResult()
        }

    override suspend fun setSavedSelection(
        selection: SavedSelection?
    ): CustomerSheetDataResult<Unit> = withContext(workContext) {
        customerAdapter.setSelectedPaymentOption(selection?.toPaymentOption()).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> =
        withContext(workContext) {
            customerAdapter.setupIntentClientSecretForCustomerAttach().toCustomerSheetDataResult()
        }
}

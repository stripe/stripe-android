package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionPaymentMethodDataSource @Inject constructor(
    private val elementsSessionManager: ElementsSessionManager,
    private val customerRepository: CustomerRepository,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetPaymentMethodDataSource {
    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return withContext(workContext) {
            elementsSessionManager.fetchElementsSession().mapCatching { elementsSession ->
                elementsSession.customer?.paymentMethods
                    ?: throw IllegalStateException("Should have payment methods!")
            }.toCustomerSheetDataResult()
        }
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): CustomerSheetDataResult<PaymentMethod> {
        return withContext(workContext) {
            elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
                customerRepository.updatePaymentMethod(
                    customerInfo = CustomerRepository.CustomerInfo(
                        id = ephemeralKey.customerId,
                        ephemeralKeySecret = ephemeralKey.ephemeralKey
                    ),
                    paymentMethodId = paymentMethodId,
                    params = params,
                ).getOrThrow()
            }.toCustomerSheetDataResult()
        }
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        throw IllegalStateException("Should not call this method!")
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return withContext(workContext) {
            elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
                customerRepository.detachPaymentMethod(
                    customerInfo = CustomerRepository.CustomerInfo(
                        id = ephemeralKey.customerId,
                        ephemeralKeySecret = ephemeralKey.ephemeralKey
                    ),
                    canRemoveDuplicates = true,
                    paymentMethodId = paymentMethodId,
                ).getOrThrow()
            }.toCustomerSheetDataResult()
        }
    }
}

package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionPaymentMethodDataSource @Inject constructor(
    private val elementsSessionManager: CustomerSessionElementsSessionManager,
    private val customerRepository: CustomerRepository,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetPaymentMethodDataSource {
    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return withContext(workContext) {
            elementsSessionManager.fetchElementsSession().mapCatching { elementsSessionWithCustomer ->
                elementsSessionWithCustomer.customer.paymentMethods
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
                        ephemeralKeySecret = ephemeralKey.ephemeralKey,
                        customerSessionClientSecret = ephemeralKey.customerSessionClientSecret,
                    ),
                    paymentMethodId = paymentMethodId,
                    params = params,
                ).getOrThrow()
            }.toCustomerSheetDataResult()
        }
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        errorReporter.report(
            errorEvent = ErrorReporter.UnexpectedErrorEvent.CUSTOMER_SHEET_ATTACH_CALLED_WITH_CUSTOMER_SESSION,
        )

        return CustomerSheetDataResult.failure(
            cause = IllegalStateException("'attach' is not supported for `CustomerSession`!"),
            displayMessage = null,
        )
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return withContext(workContext) {
            elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
                customerRepository.detachPaymentMethod(
                    customerInfo = CustomerRepository.CustomerInfo(
                        id = ephemeralKey.customerId,
                        ephemeralKeySecret = ephemeralKey.ephemeralKey,
                        customerSessionClientSecret = ephemeralKey.customerSessionClientSecret,
                    ),
                    canRemoveDuplicates = true,
                    paymentMethodId = paymentMethodId,
                ).getOrThrow()
            }.toCustomerSheetDataResult()
        }
    }
}

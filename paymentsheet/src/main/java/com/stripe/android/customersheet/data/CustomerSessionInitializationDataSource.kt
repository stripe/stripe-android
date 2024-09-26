package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionInitializationDataSource @Inject constructor(
    private val elementsSessionManager: ElementsSessionManager,
    private val savedSelectionDataSource: CustomerSessionSavedSelectionDataSource,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetInitializationDataSource {
    override suspend fun loadCustomerSheetSession(): CustomerSheetDataResult<CustomerSheetSession> {
        return withContext(workContext) {
            elementsSessionManager.fetchElementsSession().mapCatching { elementsSession ->
                val savedSelection = savedSelectionDataSource
                    .retrieveSavedSelection()
                    .toResult()
                    .getOrThrow()

                CustomerSheetSession(
                    elementsSession = elementsSession,
                    paymentMethods = elementsSession.customer?.paymentMethods
                        ?: throw IllegalStateException("`customer` should not be null!"),
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                        overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
                    ),
                    savedSelection = savedSelection,
                    permissions = CustomerPermissions(
                        canRemovePaymentMethods = when (
                            val component = elementsSession.customer?.session?.components?.customerSheet
                        ) {
                            is ElementsSession.Customer.Components.CustomerSheet.Enabled ->
                                component.isPaymentMethodRemoveEnabled
                            is ElementsSession.Customer.Components.CustomerSheet.Disabled,
                            null -> false
                        }
                    )
                )
            }.toCustomerSheetDataResult()
        }
    }
}

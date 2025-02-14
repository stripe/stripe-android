package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionInitializationDataSource @Inject constructor(
    private val elementsSessionManager: CustomerSessionElementsSessionManager,
    private val savedSelectionDataSource: CustomerSheetSavedSelectionDataSource,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetInitializationDataSource {
    override suspend fun loadCustomerSheetSession(
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetDataResult<CustomerSheetSession> {
        return withContext(workContext) {
            elementsSessionManager.fetchElementsSession().mapCatching { customerSessionElementsSession ->
                val savedSelection = savedSelectionDataSource
                    .retrieveSavedSelection(
                        elementsSession = customerSessionElementsSession
                    )
                    .toResult()
                    .getOrThrow()

                val customer = customerSessionElementsSession.customer

                val canRemoveLastPaymentMethodFromCustomerSession = when (
                    val component = customer.session.components.customerSheet
                ) {
                    is ElementsSession.Customer.Components.CustomerSheet.Enabled ->
                        component.canRemoveLastPaymentMethod
                    is ElementsSession.Customer.Components.CustomerSheet.Disabled -> false
                }

                CustomerSheetSession(
                    elementsSession = customerSessionElementsSession.elementsSession,
                    paymentMethods = customer.paymentMethods,
                    /*
                     * `CustomerSession` on `CustomerSheet` never shows the checkbox but always assumes any PMs
                     * saved through it are meant to be re-shown since `CustomerSheet` acts as a wallet version
                     * of `PaymentSheet` hence the override.
                     */
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                        overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
                    ),
                    savedSelection = savedSelection,
                    permissions = CustomerPermissions(
                        canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod &&
                            canRemoveLastPaymentMethodFromCustomerSession,
                        canRemovePaymentMethods = when (val component = customer.session.components.customerSheet) {
                            is ElementsSession.Customer.Components.CustomerSheet.Enabled ->
                                component.isPaymentMethodRemoveEnabled
                            is ElementsSession.Customer.Components.CustomerSheet.Disabled -> false
                        }
                    ),
                    defaultPaymentMethodId = customer.defaultPaymentMethod,
                )
            }.toCustomerSheetDataResult()
        }
    }
}

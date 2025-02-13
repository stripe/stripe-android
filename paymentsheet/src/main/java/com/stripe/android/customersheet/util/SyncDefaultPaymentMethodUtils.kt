package com.stripe.android.customersheet.util

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod

internal fun List<PaymentMethod>.filterToSupportedPaymentMethods(
    isSyncDefaultPaymentMethodFeatureEnabled: Boolean,
): List<PaymentMethod> {
    val paymentMethodTypesSupportedWithSyncDefaultFeature = listOf(
        PaymentMethod.Type.Card,
        PaymentMethod.Type.USBankAccount,
    )

    return this.filter { paymentMethod ->
        !isSyncDefaultPaymentMethodFeatureEnabled ||
            paymentMethodTypesSupportedWithSyncDefaultFeature.contains(paymentMethod.type)
    }
}

internal fun getDefaultPaymentMethodsEnabledForCustomerSheet(elementsSession: ElementsSession): Boolean {
    return when (val customerSheetComponent = elementsSession.customer?.session?.components?.customerSheet) {
        is ElementsSession.Customer.Components.CustomerSheet.Enabled ->
            customerSheetComponent.isPaymentMethodSyncDefaultEnabled
        ElementsSession.Customer.Components.CustomerSheet.Disabled,
        null -> false
    }
}

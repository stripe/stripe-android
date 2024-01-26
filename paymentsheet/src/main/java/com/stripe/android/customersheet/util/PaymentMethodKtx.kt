package com.stripe.android.customersheet.util

import com.stripe.android.model.PaymentMethod

internal fun PaymentMethod.isUnverifiedUSBankAccount(): Boolean {
    return type == PaymentMethod.Type.USBankAccount && usBankAccount?.financialConnectionsAccount == null
}

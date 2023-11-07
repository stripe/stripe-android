package com.stripe.android.testing

import com.stripe.android.model.PaymentMethod

object PaymentMethodFactory {

    fun card(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            code = PaymentMethod.Type.Card.code,
        )
    }

    fun cashAppPay(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.CashAppPay,
            code = PaymentMethod.Type.CashAppPay.code,
        )
    }

    fun usBankAccount(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.USBankAccount,
            code = PaymentMethod.Type.USBankAccount.code,
        )
    }

    fun sepaDebit(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.SepaDebit,
            code = PaymentMethod.Type.SepaDebit.code,
        )
    }
}

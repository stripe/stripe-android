package com.stripe.android.testing

import com.stripe.android.model.PaymentMethod
import kotlin.random.Random

object PaymentMethodFactory {

    fun cards(size: Int): List<PaymentMethod> {
        return buildList {
            repeat(size) {
                add(card(random = true))
            }
        }
    }

    fun card(random: Boolean = false): PaymentMethod {
        val id = if (random) {
            "pm_${Random.nextInt(from = 1_000, until = 10_000)}"
        } else {
            "pm_1234"
        }

        return PaymentMethod(
            id = id,
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            code = PaymentMethod.Type.Card.code,
            card = PaymentMethod.Card(
                last4 = "4242",
            ),
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

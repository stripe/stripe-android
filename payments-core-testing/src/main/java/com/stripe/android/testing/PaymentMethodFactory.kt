package com.stripe.android.testing

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

object PaymentMethodFactory {

    fun card(last4: String, id: String? = null, addCbcNetworks: Boolean = false): PaymentMethod {
        val card = if (id == null) {
            card(random = true)
        } else {
            card(id = id)
        }

        return card.run {
            update(last4, addCbcNetworks)
        }
    }

    fun PaymentMethod.update(
        last4: String?,
        addCbcNetworks: Boolean,
        brand: CardBrand = CardBrand.Visa
    ): PaymentMethod {
        return copy(
            card = card?.copy(
                last4 = last4,
                networks = PaymentMethod.Card.Networks(
                    available = setOf("cartes_bancaires", "visa"),
                    preferred = "cartes_bancaries",
                ).takeIf {
                    addCbcNetworks
                },
                displayBrand = "cartes_bancaries".takeIf { addCbcNetworks },
                brand = brand,
            )
        )
    }

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
        return card(id = id)
    }

    fun card(id: String?): PaymentMethod {
        return PaymentMethod(
            id = id,
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            code = PaymentMethod.Type.Card.code,
            card = PaymentMethod.Card(
                last4 = "4242",
                expiryMonth = 3,
                expiryYear = 2027,
            ),
        )
    }

    fun visaCard(): PaymentMethod {
        return card(random = false).update(
            last4 = "4242",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
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

    fun swish(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.Swish,
            code = PaymentMethod.Type.Swish.code,
        )
    }

    fun usBankAccount(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.USBankAccount,
            code = PaymentMethod.Type.USBankAccount.code,
            usBankAccount = PaymentMethod.USBankAccount(
                accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL,
                accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
                bankName = "STRIPE TEST BANK",
                fingerprint = "FFDMA0xfhBjWSZLu",
                last4 = "6789",
                financialConnectionsAccount = "fca_1PvmkYLu5o3P18Zp3o1YDi1z",
                networks = PaymentMethod.USBankAccount.USBankNetworks(
                    preferred = "ach",
                    supported = listOf("ach")
                ),
                routingNumber = "110000000",
            )
        )
    }

    fun instantDebits(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.Link,
            code = PaymentMethod.Type.Link.code,
        )
    }

    fun bacs(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.BacsDebit,
            code = PaymentMethod.Type.BacsDebit.code,
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

    fun amazonPay(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.AmazonPay,
            code = PaymentMethod.Type.AmazonPay.code,
        )
    }

    fun revolutPay(): PaymentMethod {
        return PaymentMethod(
            id = "pm_1234",
            created = 123456789L,
            liveMode = false,
            type = PaymentMethod.Type.RevolutPay,
            code = PaymentMethod.Type.RevolutPay.code,
        )
    }

    fun convertCardToJson(paymentMethod: PaymentMethod): JSONObject {
        val paymentMethodJson = convertGenericPaymentMethodToJson(paymentMethod)

        val card = paymentMethod.card
        val cardJson = JSONObject()

        cardJson.put("display_brand", card?.displayBrand)
        cardJson.put("brand", card?.brand?.code)
        cardJson.put("last4", card?.last4)
        cardJson.put("exp_month", card?.expiryMonth)
        cardJson.put("exp_year", card?.expiryYear)

        val networks = paymentMethod.card?.networks
        val networksJson = JSONObject()
        val availableJson = JSONArray()

        networks?.available?.forEach {
            availableJson.put(it)
        }

        if (availableJson.length() > 0) {
            networksJson.put("available", availableJson)
            networksJson.put("preferred", networks?.preferred)

            cardJson.put("networks", networksJson)
        }

        paymentMethodJson.put("card", cardJson)

        return paymentMethodJson
    }

    fun convertUsBankAccountToJson(paymentMethod: PaymentMethod): JSONObject {
        val paymentMethodJson = convertGenericPaymentMethodToJson(paymentMethod)

        val usBankAccount = paymentMethod.usBankAccount!!
        val usBankAccountJson = JSONObject()

        usBankAccountJson.put("account_holder_type", usBankAccount.accountHolderType.value)
        usBankAccountJson.put("account_type", usBankAccount.accountType.value)
        usBankAccountJson.put("bank_name", usBankAccount.bankName)
        usBankAccountJson.put("financial_connections_account", usBankAccount.financialConnectionsAccount)
        usBankAccountJson.put("fingerprint", usBankAccount.fingerprint)
        usBankAccountJson.put("last4", usBankAccount.last4)
        usBankAccountJson.put("routing_number", usBankAccount.routingNumber)

        val networksJson = JSONObject()
        networksJson.put("preferred", usBankAccount.networks?.preferred)
        val supportedJson = JSONArray()
        usBankAccount.networks?.supported?.forEach { supported ->
            supportedJson.put(supported)
        }
        networksJson.put("supported", supportedJson)
        usBankAccountJson.put("networks", networksJson)

        paymentMethodJson.put("us_bank_account", usBankAccountJson)

        return paymentMethodJson
    }

    fun convertSepaPaymentMethodToJson(paymentMethod: PaymentMethod): JSONObject {
        val paymentMethodJson = convertGenericPaymentMethodToJson(paymentMethod)

        val sepaDebitJson = JSONObject()
        // Test values from the SEPA account with the test IBAN we use for e2e tests: DE89370400440532013000
        sepaDebitJson.put("bank_code", "37040044")
        sepaDebitJson.put("branch_code", "")
        sepaDebitJson.put("country", "DE")
        sepaDebitJson.put("fingerprint", "vifs0Ho7vwRn1Miu")
        sepaDebitJson.put("last4", "3000")

        paymentMethodJson.put("type", "sepa_debit")
        paymentMethodJson.put("sepa_debit", sepaDebitJson)

        return paymentMethodJson
    }

    private fun convertGenericPaymentMethodToJson(paymentMethod: PaymentMethod): JSONObject {
        val paymentMethodJson = JSONObject()

        paymentMethodJson.put("id", paymentMethod.id)
        paymentMethodJson.put("type", paymentMethod.type?.code)
        paymentMethodJson.put("created", paymentMethod.created)
        paymentMethodJson.put("customer", paymentMethod.customerId)
        paymentMethodJson.put("livemode", paymentMethod.liveMode)

        return paymentMethodJson
    }
}

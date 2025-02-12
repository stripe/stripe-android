package com.stripe.android.network

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.json.JSONObject

internal sealed interface PaymentMethodDetails {
    val id: String
    val type: String

    fun createJson(transform: (PaymentMethod) -> PaymentMethod = { it }): JSONObject
}

internal data class CardPaymentMethodDetails(
    override val id: String,
    val last4: String,
    val addCbcNetworks: Boolean = false,
    val brand: CardBrand = CardBrand.Visa
) : PaymentMethodDetails {
    override val type: String = "card"

    fun createPaymentMethod(): PaymentMethod {
        return PaymentMethodFactory.card(
            id = id
        ).update(
            last4 = last4,
            addCbcNetworks = addCbcNetworks,
            brand = brand
        )
    }

    override fun createJson(transform: (PaymentMethod) -> PaymentMethod): JSONObject {
        return PaymentMethodFactory.convertCardToJson(transform(createPaymentMethod()))
    }
}

internal data class UsBankPaymentMethodDetails(
    override val id: String,
) : PaymentMethodDetails {
    override val type: String = "us_bank_account"

    override fun createJson(transform: (PaymentMethod) -> PaymentMethod): JSONObject {
        return PaymentMethodFactory.convertUsBankAccountToJson(PaymentMethodFactory.usBankAccount().copy(id = id))
    }
}

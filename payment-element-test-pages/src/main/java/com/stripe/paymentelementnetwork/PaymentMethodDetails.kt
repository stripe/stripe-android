package com.stripe.paymentelementnetwork

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.json.JSONObject

fun PaymentMethod.toJsonWithBillingDetails(): JSONObject {
    val billingDetails = requireNotNull(this.billingDetails)
    val address = billingDetails.address?.let { address ->
        JSONObject()
            .put("line1", address.line1)
            .put("line2", address.line2)
            .put("city", address.city)
            .put("state", address.state)
            .put("country", address.country)
            .put("postal_code", address.postalCode)
    }
    val billingDetailsJson = JSONObject()
        .put("name", billingDetails.name)
        .put("email", billingDetails.email)
        .put("phone", billingDetails.phone)
        .put("address", address)

    return PaymentMethodFactory.convertCardToJson(this)
        .put("billing_details", billingDetailsJson)
}

sealed interface PaymentMethodDetails {
    val id: String
    val type: String

    fun createJson(transform: (PaymentMethod) -> PaymentMethod = { it }): JSONObject
}

data class CardPaymentMethodDetails(
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

data class UsBankPaymentMethodDetails(
    override val id: String,
) : PaymentMethodDetails {
    override val type: String = "us_bank_account"

    override fun createJson(transform: (PaymentMethod) -> PaymentMethod): JSONObject {
        return PaymentMethodFactory.convertUsBankAccountToJson(PaymentMethodFactory.usBankAccount().copy(id = id))
    }
}

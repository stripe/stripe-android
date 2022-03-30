package com.stripe.android.connections.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object PaymentAccountSerializer :
    JsonContentPolymorphicSerializer<PaymentAccount>(PaymentAccount::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out PaymentAccount> {
        return when {
            "created" in element.jsonObject -> LinkedAccount.serializer()
            else -> BankAccount.serializer()
        }
    }
}
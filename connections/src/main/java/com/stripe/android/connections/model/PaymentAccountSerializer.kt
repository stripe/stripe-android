package com.stripe.android.connections.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

object PaymentAccountSerializer :
    JsonContentPolymorphicSerializer<PaymentAccount>(PaymentAccount::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out PaymentAccount> {
        return try {
            Json.decodeFromJsonElement(LinkedAccount.serializer(), element)
            LinkedAccount.serializer()
        } catch (e: SerializationException) {
            BankAccount.serializer()
        }
    }
}

package com.stripe.android.connections.model.serializer

import com.stripe.android.connections.model.BankAccount
import com.stripe.android.connections.model.LinkedAccount
import com.stripe.android.connections.model.PaymentAccount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

internal object PaymentAccountSerializer :
    JsonContentPolymorphicSerializer<PaymentAccount>(PaymentAccount::class) {

    @Suppress("SwallowedException")
    override fun selectDeserializer(element: JsonElement): KSerializer<out PaymentAccount> {
        return try {
            Json.decodeFromJsonElement(LinkedAccount.serializer(), element)
            LinkedAccount.serializer()
        } catch (e: SerializationException) {
            BankAccount.serializer()
        }
    }
}

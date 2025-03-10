package com.stripe.android.financialconnections.model.serializer

import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PaymentAccount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object PaymentAccountSerializer :
    JsonContentPolymorphicSerializer<PaymentAccount>(PaymentAccount::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out PaymentAccount> {
        return when (element.objectValue) {
            FinancialConnectionsAccount.Companion.OBJECT_OLD,
            FinancialConnectionsAccount.Companion.OBJECT_NEW -> FinancialConnectionsAccount.Companion.serializer()
            else -> BankAccount.serializer()
        }
    }

    /**
     * gets the `object` value from the given [JsonElement]
     */
    private val JsonElement.objectValue: String?
        get() = jsonObject["object"]?.jsonPrimitive?.content
}
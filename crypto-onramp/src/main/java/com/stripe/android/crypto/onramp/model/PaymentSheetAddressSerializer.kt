package com.stripe.android.crypto.onramp.model

import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class AddressSurrogate(
    val city: String? = null,
    val country: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val state: String? = null
)

internal object PaymentSheetAddressSerializer : KSerializer<PaymentSheet.Address> {
    private val delegate = AddressSurrogate.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: PaymentSheet.Address) {
        val surrogate = AddressSurrogate(
            city = value.city,
            country = value.country,
            line1 = value.line1,
            line2 = value.line2,
            postalCode = value.postalCode,
            state = value.state
        )

        delegate.serialize(encoder, surrogate)
    }

    override fun deserialize(decoder: Decoder): PaymentSheet.Address {
        val surrogate = delegate.deserialize(decoder)

        return PaymentSheet.Address.Builder()
            .city(surrogate.city)
            .country(surrogate.country)
            .line1(surrogate.line1)
            .line2(surrogate.line2)
            .postalCode(surrogate.postalCode)
            .state(surrogate.state)
            .build()
    }
}

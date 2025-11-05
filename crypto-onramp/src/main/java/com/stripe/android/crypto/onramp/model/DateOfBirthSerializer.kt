package com.stripe.android.crypto.onramp.model

import com.stripe.android.model.DateOfBirth
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class DateOfBirthSurrogate(
    val day: Int,
    val month: Int,
    val year: Int
)

internal object DateOfBirthSerializer : KSerializer<DateOfBirth> {
    private val delegate = DateOfBirthSurrogate.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: DateOfBirth) {
        val surrogate = DateOfBirthSurrogate(
            day = value.day,
            month = value.month,
            year = value.year
        )

        delegate.serialize(encoder, surrogate)
    }

    override fun deserialize(decoder: Decoder): DateOfBirth {
        val surrogate = delegate.deserialize(decoder)

        return DateOfBirth(
            day = surrogate.day,
            month = surrogate.month,
            year = surrogate.year
        )
    }
}

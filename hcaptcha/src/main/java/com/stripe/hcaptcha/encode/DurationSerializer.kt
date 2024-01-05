package com.stripe.hcaptcha.encode

import androidx.annotation.RestrictTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationInSeconds", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Duration =
        decoder.decodeLong().seconds

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeSeconds)
    }
}

package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias IntAsRgbHexString = @Serializable(IntAsRgbHexStringSerializer::class) Int

object IntAsRgbHexStringSerializer : KSerializer<Int> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntAsRgbHexString", PrimitiveKind.STRING)

    @Suppress("MagicNumber")
    override fun serialize(encoder: Encoder, value: Int) {
        // Convert the integer to a hex color string
        val hexString = "#%06X".format(0xFFFFFF and value)
        encoder.encodeString(hexString)
    }

    @Suppress("MagicNumber")
    override fun deserialize(decoder: Decoder): Int {
        // Decode the hex color string back to an integer
        val hexString = decoder.decodeString()
        return hexString.removePrefix("#").toInt(16)
    }
}

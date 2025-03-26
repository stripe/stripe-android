package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Indicates that the integer should be serialized as a hex color string, e.g. "#FF0000".
 * Note that typealias's such as this are only serializable when in a class, and cannot be
 * directly serialized on their own. See https://youtrack.jetbrains.com/issue/KT-64920/Json.encodeToString-yields-different-results-depending-on-whether-typealias-is-used#focus=Comments-27-9092520.0-0.
 */
internal typealias IntAsRgbHexString =
    @Serializable(IntAsRgbHexStringSerializer::class)
    Int

/**
 * A [KSerializer] for serializing an [Int] as a hex color string, e.g. "#FF0000".
 */
internal object IntAsRgbHexStringSerializer : KSerializer<Int> {

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

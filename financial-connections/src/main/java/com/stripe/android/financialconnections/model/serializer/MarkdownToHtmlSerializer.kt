package com.stripe.android.financialconnections.model.serializer

import com.stripe.android.financialconnections.utils.MarkdownParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object MarkdownToHtmlSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MarkdownToHtml", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String): Unit = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = MarkdownParser.toHtml(decoder.decodeString())
}

package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MarkdownToHtmlSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MarkdownToHtml", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String): Unit = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = MarkdownParser.toHtml(decoder.decodeString())
}

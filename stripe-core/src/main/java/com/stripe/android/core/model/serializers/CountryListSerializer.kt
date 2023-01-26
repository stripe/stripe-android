package com.stripe.android.core.model.serializers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection

/**
 * A customized [KSerializer] to convert between a JSON of Map<String, String> into List<Country>.
 * E.g {"US": "United States","AU": "Australia"} will be serialized to List<Country>(US, AU).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CountryListSerializer : KSerializer<List<Country>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = mapSerialDescriptor<String, String>()

    override fun deserialize(decoder: Decoder): List<Country> {
        val ret = mutableListOf<Country>()
        val compositeDecoder = decoder.beginStructure(descriptor)
        while (true) {
            val index = compositeDecoder.decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            val code = compositeDecoder.decodeStringElement(descriptor, index)
            val name = compositeDecoder.decodeStringElement(
                descriptor,
                compositeDecoder.decodeElementIndex(
                    descriptor
                )
            )
            ret.add(Country(CountryCode(code), name))
        }
        compositeDecoder.endStructure(descriptor)
        return ret
    }

    override fun serialize(encoder: Encoder, value: List<Country>) {
        encoder.encodeCollection(descriptor, value.size) {
            val iterator = value.iterator()
            var index = 0
            iterator.forEach { (code, name) ->
                encodeStringElement(descriptor, index++, code.value)
                encodeStringElement(descriptor, index++, name)
            }
        }
    }
}

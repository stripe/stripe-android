package com.stripe.android.paymentsheet.address

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable(with = FieldTypeAsStringSerializer::class)
internal enum class FieldType(val serializedValue: String) {
    AddressLine1("addressLine1"),
    AddressLine2("addressLine2"),
    Locality("locality"),
    PostalCode("postalCode"),
    AdministrativeArea("administrativeArea"),
    Name("name");

    companion object {
        fun from(value: String) = values().firstOrNull {
            it.serializedValue == value
        }
    }
}

internal enum class NameType(@StringRes val stringResId: Int) {
    area(R.string.address_label_hk_area),
    cedex(R.string.address_label_cedex),
    city(R.string.address_label_city),
    country(R.string.address_label_country),
    county(R.string.address_label_county),
    department(R.string.address_label_department),
    district(R.string.address_label_district),
    do_si(R.string.address_label_kr_do_si),
    eircode(R.string.address_label_ie_eircode),
    emirate(R.string.address_label_ae_emirate),
    island(R.string.address_label_island),
    neighborhood(R.string.address_label_neighborhood),
    oblast(R.string.address_label_oblast),
    parish(R.string.address_label_bb_jm_parish),
    pin(R.string.address_label_in_pin),
    post_town(R.string.address_label_post_town),
    postal(R.string.address_label_postal_code),
    prefecture(R.string.address_label_jp_prefecture),
    province(R.string.address_label_province),
    state(R.string.address_label_state),
    suburb(R.string.address_label_suburb),
    suburb_or_city(R.string.address_label_au_suburb_or_city),
    townland(R.string.address_label_ie_townland),
    village_township(R.string.address_label_village_township),
    zip(R.string.address_label_zip_code)
}

@Serializable
internal class StateSchema(
    val isoID: String, // sometimes empty string (i.e. Armed Forces (AP))
    val key: String, // abbreviation: TODO: How is it used?
    val name: String, // display name
)

@Serializable
internal class FieldSchema(
    val isNumeric: Boolean = false,
    val examples: List<String> = emptyList(),
    val nameType: NameType, // label,
)

@Serializable
internal class AddressSchema(
    val type: FieldType?,
    val required: Boolean,
    val schema: FieldSchema? = null
)

private val format = Json { ignoreUnknownKeys = true }

internal fun parseAddressesSchema(inputStream: InputStream?) =
    getJsonStringFromInputStream(inputStream)?.let {
        format.decodeFromString<List<AddressSchema>>(
            it
        )
    }

private object FieldTypeAsStringSerializer : KSerializer<FieldType?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FieldType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FieldType?) {
        encoder.encodeString(value?.serializedValue ?: "")
    }

    override fun deserialize(decoder: Decoder): FieldType? {
        return FieldType.from(decoder.decodeString())
    }
}

private fun getJsonStringFromInputStream(inputStream: InputStream?) =
    inputStream?.bufferedReader().use { it?.readText() }

internal fun List<AddressSchema>.transformToSpecFieldList() =
    this.mapNotNull {
        when (it.type) {
            FieldType.AddressLine1 -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("line1"),
                    it.schema?.nameType?.stringResId ?: R.string.address_label_address,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = getKeyboard(it.schema),
                    showOptionalLabel = !it.required
                )
            }
            FieldType.AddressLine2 -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("line2"),
                    it.schema?.nameType?.stringResId ?: R.string.address_label_address_line2,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = getKeyboard(it.schema),
                    showOptionalLabel = !it.required
                )
            }
            FieldType.Locality -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("city"),
                    it.schema?.nameType?.stringResId ?: R.string.address_label_city,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = getKeyboard(it.schema),
                    showOptionalLabel = !it.required
                )
            }
            FieldType.AdministrativeArea -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("state"),
                    it.schema?.nameType?.stringResId ?: NameType.state.stringResId,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = getKeyboard(it.schema),
                    showOptionalLabel = !it.required
                )
            }
            FieldType.PostalCode -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("postal_code"),
                    it.schema?.nameType?.stringResId ?: R.string.address_label_postal_code,
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = getKeyboard(it.schema),
                    showOptionalLabel = !it.required
                )
            }
            else -> null
        }
    }

private fun getKeyboard(fieldSchema: FieldSchema?) = if (fieldSchema?.isNumeric == true) {
    KeyboardType.Number
} else {
    KeyboardType.Text
}

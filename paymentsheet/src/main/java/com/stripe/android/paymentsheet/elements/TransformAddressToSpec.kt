package com.stripe.android.paymentsheet.elements

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
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
import java.io.IOException
import java.io.InputStream

@Serializable(with = FieldTypeAsStringSerializer::class)
enum class FieldType(val serializedValue: String) {
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

enum class NameType(@StringRes val stringResId: Int) {
    city(R.string.address_label_city),
    island(R.string.address_label_island),
    province(R.string.address_label_province),
    country(R.string.address_label_country),
    state(R.string.address_label_state),
    postal(R.string.address_label_postal_code),
    cedex(R.string.address_label_cedex),
    suburb(R.string.address_label_suburb),
    oblast(R.string.address_label_oblast),
    district(R.string.address_label_district),
    post_town(R.string.address_label_post_town),
    department(R.string.address_label_department),
    village_township(R.string.address_label_village_township),
    neighborhood(R.string.address_label_neighborhood),
    do_si(R.string.address_label_kr_do_si),
    prefecture(R.string.address_label_jp_prefecture),
    parish(R.string.address_label_bb_jm_parish),
    townland(R.string.address_label_ie_townland),
    suburb_or_city(R.string.address_label_au_suburb_or_city),
    emirate(R.string.address_label_ae_emirate),
    zip(R.string.acc_label_zip)
}

@Serializable
class PostalCodeRegex

@Serializable
class StateSchema(
    val isoID: String, // sometimes empty string (i.e. Armed Forces (AP))
    val key: String, // abbreviation: TODO: How is it used?
    val name: String, // display name
//    val latinName: String? = null,// value when not english
//    val postalCodeRegex: PostalCodeRegex? = null,//  Always an empty object
//    val postalCodeExamples: List<String> = emptyList()
)

@Serializable
class FieldSchema(
//    val regex: String = ".*", //always null
    val isNumeric: Boolean = false,
    val examples: List<String> = emptyList(),
    val nameType: NameType, // label,
//    val list: List<StateSchema> = emptyList()
)

@Serializable
class AddressSchema(
    val type: FieldType?,
    val required: Boolean,
    val schema: FieldSchema? = null
)

val format = Json { ignoreUnknownKeys = true }

internal fun parseAddressesSchema(resources: Resources, assetFileName: String) =
    parseAddressesSchema(
        resources.assets.open(assetFileName)
    )

@VisibleForTesting
internal fun parseAddressesSchema(inputStream: InputStream?) =
    try {

        getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<List<AddressSchema>>(
                it
            )
        }
    } catch (e: Exception) {
        println("Error parsing: " + e.localizedMessage)
        null
    }

object FieldTypeAsStringSerializer : KSerializer<FieldType?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FieldType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FieldType?) {
        encoder.encodeString(value?.serializedValue ?: "")
    }

    override fun deserialize(decoder: Decoder): FieldType? {
        return FieldType.from(decoder.decodeString())
    }
}

private fun getJsonStringFromInputStream(inputStream: InputStream?): String? {
    val jsonString: String?
    try {
        jsonString = inputStream?.bufferedReader().use { it?.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return null
    }
    return jsonString
}

internal fun List<AddressSchema>.transformToSpecFieldList() =
    this.mapNotNull {
        when (it.type) {
            FieldType.AddressLine1 -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("line1"),
                    R.string.address_label_address_line1,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            }
            FieldType.AddressLine2 -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("line2"),
                    R.string.address_label_address_line2,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            }
            FieldType.Locality -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("city"),
                    R.string.address_label_city,
//                    it.required,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            }
            FieldType.AdministrativeArea -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("state"),
                    it.schema?.nameType?.stringResId ?: NameType.state.stringResId,
//                    it.required,
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            }
            FieldType.PostalCode -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("postal_code"),
                    R.string.address_label_postal_code,
//                    it.required,
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text
                )
            }
            else -> null
        }
    }

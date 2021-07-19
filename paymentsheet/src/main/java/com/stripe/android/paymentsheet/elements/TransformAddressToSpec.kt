package com.stripe.android.paymentsheet.elements

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
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

@Serializable
class LibAddressInput(
    val id: String,
    val key: String,
    val name: String,
    val lang: String? = null,
    val languages: String? = null,
    val fmt: String? = null,
    val require: String? = null,
    val zip: String? = null,
    val sub_keys: String? = null,
    val sub_names: String? = null,
    val sub_zips: String? = null,
    val sub_isoids: String? = null,
    val state_name_type: String? = null
)

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
    zip(R.string.address_label_zip_code)
}

@Serializable
class PostalCodeRegex {}

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
    val nameType: NameType, //label,
//    val list: List<StateSchema> = emptyList()
)

@Serializable
class AddressSchema(
    val type: FieldType?,
    val required: Boolean,
    val schema: FieldSchema? = null
)

val format = Json { ignoreUnknownKeys = true }

internal fun parseAddressesSchema(context: Context, assetFileName: String) =
    parseAddressesSchema(
        context.assets.open(assetFileName)
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
                    it.required
                )
            }
            FieldType.AddressLine2 -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("line2"),
                    R.string.address_label_address_line2,
                    false,
                )
            }
            FieldType.Locality -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("city"),
                    R.string.address_label_city,
                    it.required
                )
            }
            FieldType.AdministrativeArea -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("state"),
                    it.schema?.nameType?.stringResId ?: NameType.state.stringResId,
                    it.required
                )
            }
            FieldType.PostalCode -> {
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("postal_code"),
                    R.string.address_label_postal_code,
                    it.required
                )
            }
            FieldType.Name -> {
                SectionFieldSpec.Email
            }
            else -> null
        }
    }


/**
 * AE.json:  "state_name_type": "emirate",
 * AU.json:  "state_name_type": "state",
 * BR.json:  "state_name_type": "state",
 * HK.json:  "state_name_type": "area",
 * IE.json:  "state_name_type": "county",
 * IN.json:  "state_name_type": "state",
 * JP.json:  "state_name_type": "prefecture",
 * MX.json:  "state_name_type": "state",
 * US.json:  "state_name_type": "state",
 */
//private fun transformState(value: String?) = when (value) {
//    "state" -> R.string.address_label_state
//    "perfecture" -> R.string.address_label_perfecture
//    "emirate" -> R.string.address_label_ae_emirate
//    "area" -> R.string.address_label_hk_area
//    "county" -> R.string.address_label_ie_county
//    else -> null
//}
//
//internal fun parseLibAddressInput(context: Context, assetFileName: String) =
//    parseLibAddressInput(
//        context.assets.open(assetFileName)
//    )
//
//@VisibleForTesting
//internal fun parseLibAddressInput(inputStream: InputStream?) =
//    try {
//        getJsonStringFromInputStream(inputStream)?.let {
//            format.decodeFromString<LibAddressInput>(
//                it
//            )
//        }
//    } catch (e: Exception) {
//        println("Error parsing: " + e.localizedMessage)
//        null
//    }
//
//

//private fun getFieldTypes(fmtString: String?) =
//    fmtString?.let {
//        Regex("%([A-Z])").findAll(fmtString).map { it.groupValues[1] }
//    }
//
//internal fun LibAddressInput.transformToSpecFieldList() =
//    getFieldTypes(this.fmt)
//        ?.map {
//            when (it) {
//                "A" -> {
//                    listOf(
//                        SectionFieldSpec.GenericText(
//                            IdentifierSpec("line1"),
//                            R.string.address_label_address_line1,
//                            this.require?.contains("A") == true
//                        ),
//
//                        SectionFieldSpec.GenericText(
//                            IdentifierSpec("line2"),
//                            R.string.address_label_address_line2,
//                            false,
//                        )
//                    )
//                }
//                "C" -> {
//                    listOf(
//                        SectionFieldSpec.GenericText(
//                            IdentifierSpec("city"),
//                            R.string.address_label_city,
//                            this.require?.contains("C") == true
//                        )
//                    )
//                }
//                "S" -> {
//                    listOf(
//                        SectionFieldSpec.GenericText(
//                            IdentifierSpec("state"),
//                            // TODO: Allow a default string
//                            transformState(this.state_name_type)
//                                ?: R.string.address_label_state,
//                            this.require?.contains("S") == true
//                        )
//                    )
//                }
//                "Z" -> {
//                    listOf(
//                        SectionFieldSpec.GenericText(
//                            IdentifierSpec("postal_code"),
//                            R.string.address_label_postal_code,
//                            this.require?.contains("Z") == true
//                        )
//                    )
//                }
//                "D" -> emptyList()
//                "X" -> emptyList()
//                "O" -> emptyList()
//                else -> emptyList()
//            }
//        }
//        ?.flatten()

package com.stripe.android.ui.core.address

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.RowController
import com.stripe.android.ui.core.elements.RowElement
import com.stripe.android.ui.core.elements.SectionFieldElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.SimpleTextElement
import com.stripe.android.ui.core.elements.SimpleTextFieldConfig
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.UUID

@Serializable(with = FieldTypeAsStringSerializer::class)
internal enum class FieldType(
    val serializedValue: String,
    val identifierSpec: IdentifierSpec,
    @StringRes val defaultLabel: Int,
    val capitalization: KeyboardCapitalization
) {
    AddressLine1(
        "addressLine1",
        IdentifierSpec.Line1,
        R.string.address_label_address_line1,
        KeyboardCapitalization.Words
    ),
    AddressLine2(
        "addressLine2",
        IdentifierSpec.Line2,
        R.string.address_label_address_line2,
        KeyboardCapitalization.Words
    ),
    Locality(
        "locality",
        IdentifierSpec.City,
        R.string.address_label_city,
        KeyboardCapitalization.Words
    ),
    PostalCode(
        "postalCode",
        IdentifierSpec.PostalCode,
        R.string.address_label_postal_code,
        KeyboardCapitalization.None
    ),
    AdministrativeArea(
        "administrativeArea",
        IdentifierSpec.State,
        NameType.State.stringResId,
        KeyboardCapitalization.Words
    ),
    Name(
        "name",
        IdentifierSpec.Name,
        R.string.address_label_name,
        KeyboardCapitalization.Words
    );

    companion object {
        fun from(value: String) = values().firstOrNull {
            it.serializedValue == value
        }
    }
}

@Serializable
internal enum class NameType(@StringRes val stringResId: Int) {
    @SerialName("area")
    Area(R.string.address_label_hk_area),

    @SerialName("cedex")
    Cedex(R.string.address_label_cedex),

    @SerialName("city")
    City(R.string.address_label_city),

    @SerialName("country")
    Country(R.string.address_label_country_or_region),

    @SerialName("county")
    County(R.string.address_label_county),

    @SerialName("department")
    Department(R.string.address_label_department),

    @SerialName("district")
    District(R.string.address_label_district),

    @SerialName("do_si")
    DoSi(R.string.address_label_kr_do_si),

    @SerialName("eircode")
    Eircode(R.string.address_label_ie_eircode),

    @SerialName("emirate")
    Emirate(R.string.address_label_ae_emirate),

    @SerialName("island")
    Island(R.string.address_label_island),

    @SerialName("neighborhood")
    Neighborhood(R.string.address_label_neighborhood),

    @SerialName("oblast")
    Oblast(R.string.address_label_oblast),

    @SerialName("parish")
    Parish(R.string.address_label_bb_jm_parish),

    @SerialName("pin")
    Pin(R.string.address_label_in_pin),

    @SerialName("post_town")
    PostTown(R.string.address_label_post_town),

    @SerialName("postal")
    Postal(R.string.address_label_postal_code),

    @SerialName("prefecture")
    Perfecture(R.string.address_label_jp_prefecture),

    @SerialName("province")
    Province(R.string.address_label_province),

    @SerialName("state")
    State(R.string.address_label_state),

    @SerialName("suburb")
    Suburb(R.string.address_label_suburb),

    @SerialName("suburb_or_city")
    SuburbOrCity(R.string.address_label_au_suburb_or_city),

    @SerialName("townland")
    Townload(R.string.address_label_ie_townland),

    @SerialName("village_township")
    VillageTownship(R.string.address_label_village_township),

    @SerialName("zip")
    Zip(R.string.address_label_zip_code)
}

@Serializable
internal class StateSchema(
    @SerialName("key")
    val key: String, // abbreviation
    @SerialName("name")
    val name: String // display name
)

@Serializable
internal class FieldSchema(
    @SerialName("isNumeric")
    val isNumeric: Boolean = false,
    @SerialName("examples")
    val examples: List<String> = emptyList(),
    @SerialName("nameType")
    val nameType: NameType // label,
)

@Serializable
internal class CountryAddressSchema(
    @SerialName("type")
    val type: FieldType?,
    @SerialName("required")
    val required: Boolean,
    @SerialName("schema")
    val schema: FieldSchema? = null
)

private val format = Json { ignoreUnknownKeys = true }

internal fun parseAddressesSchema(inputStream: InputStream?) =
    getJsonStringFromInputStream(inputStream)?.let {
        format.decodeFromString<List<CountryAddressSchema>>(
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

internal fun List<CountryAddressSchema>.transformToElementList(): List<SectionFieldElement> {
    val countryAddressElements = this.mapNotNull { addressField ->
        addressField.type?.let {
            SimpleTextElement(
                addressField.type.identifierSpec,
                SimpleTextFieldController(
                    SimpleTextFieldConfig(
                        label = addressField.schema?.nameType?.stringResId ?: it.defaultLabel,
                        capitalization = it.capitalization,
                        keyboard = getKeyboard(addressField.schema)
                    ),
                    showOptionalLabel = !addressField.required
                )
            )
        }
    }

    // Put it in a single row
    return combineCityAndPostal(countryAddressElements)
}

private fun combineCityAndPostal(countryAddressElements: List<SectionSingleFieldElement>) =
    countryAddressElements.foldIndexed(
        listOf<SectionFieldElement?>()
    ) { index, acc, sectionSingleFieldElement ->
        if (index + 1 < countryAddressElements.size && isPostalNextToCity(
                countryAddressElements[index],
                countryAddressElements[index + 1]
            )
        ) {
            val rowFields = listOf(countryAddressElements[index], countryAddressElements[index + 1])
            acc.plus(
                RowElement(
                    IdentifierSpec.Generic("row_" + UUID.randomUUID().leastSignificantBits),
                    rowFields,
                    RowController(rowFields)
                )
            )
        } else if (acc.lastOrNull() is RowElement) {
            // skip this it is in a row
            acc.plus(null)
        } else {
            acc.plus(sectionSingleFieldElement)
        }
    }.filterNotNull()

private fun isPostalNextToCity(
    element1: SectionSingleFieldElement,
    element2: SectionSingleFieldElement
) = isCityOrPostal(element1.identifier) && isCityOrPostal(element2.identifier)

private fun isCityOrPostal(identifierSpec: IdentifierSpec) =
    identifierSpec == IdentifierSpec.PostalCode ||
        identifierSpec == IdentifierSpec.City

private fun getKeyboard(fieldSchema: FieldSchema?) = if (fieldSchema?.isNumeric == true) {
    KeyboardType.NumberPassword
} else {
    KeyboardType.Text
}

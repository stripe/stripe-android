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
import com.stripe.android.ui.core.elements.SimpleTextSpec
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
        NameType.state.stringResId,
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
internal class CountryAddressSchema(
    val type: FieldType?,
    val required: Boolean,
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
            SimpleTextSpec(
                addressField.type.identifierSpec,
                addressField.schema?.nameType?.stringResId ?: it.defaultLabel,
                capitalization = it.capitalization,
                keyboardType = getKeyboard(addressField.schema),
                showOptionalLabel = !addressField.required
            )
        }
    }.map {
        it.transform()
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

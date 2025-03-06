package com.stripe.android.uicore.address

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import com.stripe.android.uicore.elements.AdministrativeAreaElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.utils.asIndividualDigits
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.UUID
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
enum class FieldType(
    val serializedValue: String,
    val identifierSpec: IdentifierSpec,
    @StringRes val defaultLabel: Int
) {
    @SerialName("addressLine1")
    AddressLine1(
        "addressLine1",
        IdentifierSpec.Line1,
        CoreR.string.stripe_address_label_address_line1
    ),

    @SerialName("addressLine2")
    AddressLine2(
        "addressLine2",
        IdentifierSpec.Line2,
        R.string.stripe_address_label_address_line2
    ),

    @SerialName("locality")
    Locality(
        "locality",
        IdentifierSpec.City,
        CoreR.string.stripe_address_label_city
    ),

    @SerialName("dependentLocality")
    DependentLocality(
        "dependentLocality",
        IdentifierSpec.DependentLocality,
        CoreR.string.stripe_address_label_city
    ),

    @SerialName("postalCode")
    PostalCode(
        "postalCode",
        IdentifierSpec.PostalCode,
        CoreR.string.stripe_address_label_postal_code
    ) {
        override fun capitalization() = KeyboardCapitalization.None
    },

    @SerialName("sortingCode")
    SortingCode(
        "sortingCode",
        IdentifierSpec.SortingCode,
        CoreR.string.stripe_address_label_postal_code
    ) {
        override fun capitalization() = KeyboardCapitalization.None
    },

    @SerialName("administrativeArea")
    AdministrativeArea(
        "administrativeArea",
        IdentifierSpec.State,
        NameType.State.stringResId
    ),

    @SerialName("name")
    Name(
        "name",
        IdentifierSpec.Name,
        CoreR.string.stripe_address_label_full_name
    );

    open fun capitalization() = KeyboardCapitalization.Words

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun from(value: String) = entries.firstOrNull {
            it.serializedValue == value
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
enum class NameType(@StringRes val stringResId: Int) {
    @SerialName("area")
    Area(R.string.stripe_address_label_hk_area),

    @SerialName("cedex")
    Cedex(R.string.stripe_address_label_cedex),

    @SerialName("city")
    City(CoreR.string.stripe_address_label_city),

    @SerialName("country")
    Country(CoreR.string.stripe_address_label_country_or_region),

    @SerialName("county")
    County(CoreR.string.stripe_address_label_county),

    @SerialName("department")
    Department(R.string.stripe_address_label_department),

    @SerialName("district")
    District(R.string.stripe_address_label_district),

    @SerialName("do_si")
    DoSi(R.string.stripe_address_label_kr_do_si),

    @SerialName("eircode")
    Eircode(R.string.stripe_address_label_ie_eircode),

    @SerialName("emirate")
    Emirate(R.string.stripe_address_label_ae_emirate),

    @SerialName("island")
    Island(R.string.stripe_address_label_island),

    @SerialName("neighborhood")
    Neighborhood(R.string.stripe_address_label_neighborhood),

    @SerialName("oblast")
    Oblast(R.string.stripe_address_label_oblast),

    @SerialName("parish")
    Parish(R.string.stripe_address_label_bb_jm_parish),

    @SerialName("pin")
    Pin(R.string.stripe_address_label_in_pin),

    @SerialName("post_town")
    PostTown(R.string.stripe_address_label_post_town),

    @SerialName("postal")
    Postal(CoreR.string.stripe_address_label_postal_code),

    @SerialName("prefecture")
    Perfecture(R.string.stripe_address_label_jp_prefecture),

    @SerialName("province")
    Province(CoreR.string.stripe_address_label_province),

    @SerialName("state")
    State(CoreR.string.stripe_address_label_state),

    @SerialName("suburb")
    Suburb(R.string.stripe_address_label_suburb),

    @SerialName("suburb_or_city")
    SuburbOrCity(R.string.stripe_address_label_au_suburb_or_city),

    @SerialName("townland")
    Townload(R.string.stripe_address_label_ie_townland),

    @SerialName("village_township")
    VillageTownship(R.string.stripe_address_label_village_township),

    @SerialName("zip")
    Zip(CoreR.string.stripe_address_label_zip_code)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
class StateSchema(
    @SerialName("key")
    val key: String, // abbreviation
    @SerialName("name")
    val name: String // display name
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
class FieldSchema(
    @SerialName("isNumeric")
    val isNumeric: Boolean = false,
    @SerialName("examples")
    val examples: ArrayList<String> = arrayListOf(),
    @SerialName("nameType")
    val nameType: NameType // label,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
class CountryAddressSchema(
    @SerialName("type")
    val type: FieldType?,
    @SerialName("required")
    val required: Boolean,
    @SerialName("schema")
    val schema: FieldSchema? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun parseAddressesSchema(inputStream: InputStream?) =
    getJsonStringFromInputStream(inputStream)?.let {
        format.decodeFromString(
            ListSerializer(CountryAddressSchema.serializer()),
            it
        )
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun List<CountryAddressSchema>.transformToElementList(
    countryCode: String
): List<SectionFieldElement> {
    val countryAddressElements = this
        .filterNot {
            it.type == FieldType.SortingCode ||
                it.type == FieldType.DependentLocality
        }
        .mapNotNull { addressField ->
            addressField.type?.toElement(
                identifierSpec = addressField.type.identifierSpec,
                label = addressField.schema?.nameType?.stringResId
                    ?: addressField.type.defaultLabel,
                capitalization = addressField.type.capitalization(),
                keyboardType = getKeyboard(addressField.schema),
                countryCode = countryCode,
                showOptionalLabel = !addressField.required
            )
        }

    // Put it in a single row
    return combineCityAndPostal(countryAddressElements)
}

private val format = Json { ignoreUnknownKeys = true }

private fun getJsonStringFromInputStream(inputStream: InputStream?) =
    inputStream?.bufferedReader().use { it?.readText() }

private fun FieldType.toElement(
    identifierSpec: IdentifierSpec,
    label: Int,
    capitalization: KeyboardCapitalization,
    keyboardType: KeyboardType,
    countryCode: String,
    showOptionalLabel: Boolean
): SectionSingleFieldElement {
    val simpleTextElement = SimpleTextElement(
        identifierSpec,
        SimpleTextFieldController(
            textFieldConfig = toConfig(
                label = label,
                capitalization = capitalization,
                keyboardType = keyboardType,
                countryCode = countryCode
            ),
            overrideContentDescriptionProvider = getOverrideContentDescription(),
            showOptionalLabel = showOptionalLabel
        )
    )
    return when (this) {
        FieldType.AdministrativeArea -> {
            val supportsAdministrativeAreaDropdown = listOf(
                "CA",
                "US"
            ).contains(countryCode)
            if (supportsAdministrativeAreaDropdown) {
                val country = when (countryCode) {
                    "CA" -> AdministrativeAreaConfig.Country.Canada()
                    "US" -> AdministrativeAreaConfig.Country.US()
                    else -> throw IllegalArgumentException()
                }
                AdministrativeAreaElement(
                    identifierSpec,
                    DropdownFieldController(
                        AdministrativeAreaConfig(country)
                    )
                )
            } else {
                simpleTextElement
            }
        }
        else -> simpleTextElement
    }
}

private fun FieldType.toConfig(
    label: Int,
    capitalization: KeyboardCapitalization,
    keyboardType: KeyboardType,
    countryCode: String
): TextFieldConfig {
    return when (this) {
        FieldType.PostalCode -> PostalCodeConfig(
            label = label,
            country = countryCode
        )
        else -> SimpleTextFieldConfig(
            label = label,
            capitalization = capitalization,
            keyboard = keyboardType
        )
    }
}

private fun FieldType.getOverrideContentDescription(): ((fieldValue: String) -> ResolvableString)? {
    return when (this) {
        FieldType.PostalCode -> {
            { it.asIndividualDigits().resolvableString }
        }
        else -> null
    }
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

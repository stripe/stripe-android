package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.paymentsheet.elements.BillingSectionController
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.parseAddressesSchema
import com.stripe.android.paymentsheet.elements.transformToSpecFieldList
import com.stripe.android.paymentsheet.forms.transform
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import kotlinx.coroutines.flow.map


private val supportedCountries = setOf(
    "AE",
    "AT",
    "AU",
    "BE",
    "BG",
    "BR",
    "CA",
    "CH",
    "CI",
    "CR",
    "CY",
    "CZ",
    "DE",
    "DK",
    "DO",
    "EE",
    "ES",
    "FI",
    "FR",
    "GB",
    "GI",
    "GR",
    "GT",
    "HK",
    "HU",
    "ID",
    "IE",
    "IN",
    "IT",
    "JP",
    "LI",
    "LT",
    "LU",
    "LV",
    "MT",
    "MX",
    "MY",
    "NL",
    "NO",
    "NZ",
    "PE",
    "PH",
    "PL",
    "PT",
    "RO",
    "SE",
    "SG",
    "SI",
    "SK",
    "SN",
    "TH",
    "TT",
    "US",
    "UY",
    "ZZ"
)

enum class BillingSectionFieldRepository {
    INSTANCE;

    private val countryFieldMap = mutableMapOf<String, List<SectionFieldSpec>?>()

    fun get(countryCode: String?) = countryCode?.let {
        countryFieldMap[it]
    } ?: countryFieldMap["ZZ"]

    fun init(context: Context) {
        // TODO: Supported countries should be shared with the dropdown list of countries
        val defaultCountrySections = requireNotNull(
            parseAddressesSchema(
                context,
                "addressinfo/ZZ.json"
            )?.let {
                it.transformToSpecFieldList()
            }
        )

        for (countryCode in supportedCountries) {
            countryFieldMap[countryCode] = requireNotNull(
                parseAddressesSchema(
                    context,
                    "addressinfo/${countryCode}.json"
                )?.let {
                    it.transformToSpecFieldList()
                } ?: defaultCountrySections
            )
        }
    }
}

internal class BillingSectionElement(
    override val identifier: IdentifierSpec,
    val billingSectionFieldRepository: BillingSectionFieldRepository
) : FormElement() {
    val focusRequesterCount = FocusRequesterCount()

    /**
     * Focus requester is a challenge - Must get this working from spec
     * other fields need to flow
     */

    val countryElement = SectionFieldElement.Country(
        IdentifierSpec("country"),
        DropdownFieldController(CountryConfig())
    )

    private val otherFields = countryElement.controller.rawFieldValue
        .map { countryCode ->
            billingSectionFieldRepository.get(countryCode)
                ?.let {
                    transform(
                        it,
                        FocusRequesterCount()
                    )
                } ?: emptyList()
        }


    val fields = otherFields.map { listOf(countryElement).plus(it) }
    private val fieldErrors = fields?.map { sectionFields ->
        sectionFields.map { it.controller.error }
    }
    private val fieldControllers = fields.map { sectionFields ->
        sectionFields.map { it.controller }
    }

    override val controller = BillingSectionController(
        R.string.billing_details,
        fieldControllers
    )
}

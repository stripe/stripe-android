package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AdministrativeAreaConfig(
    country: Country
) : DropdownConfig {
    private val shortAdministrativeAreaNames = country.administrativeAreas.map { it.first }
    private val fullAdministrativeAreaNames = country.administrativeAreas.map { it.second }

    override val tinyMode: Boolean = false
    override val debugLabel = "administrativeArea"

    @StringRes
    override val label = country.label

    override val rawItems = shortAdministrativeAreaNames

    override val displayItems: List<String> = fullAdministrativeAreaNames

    override val showSearch: Boolean
        get() = false

    override fun getSelectedItemLabel(index: Int) = fullAdministrativeAreaNames[index]

    override fun convertFromRaw(rawValue: String): String {
        return if (shortAdministrativeAreaNames.contains(rawValue)) {
            fullAdministrativeAreaNames[shortAdministrativeAreaNames.indexOf(rawValue)]
        } else {
            fullAdministrativeAreaNames[0]
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Country(
        open val label: Int,
        open val administrativeAreas: List<Pair<String, String>>
    ) {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Canada(
            override val label: Int = CoreR.string.stripe_address_label_province,
            override val administrativeAreas: List<Pair<String, String>> = listOf(
                Pair("AB", "Alberta"),
                Pair("BC", "British Columbia"),
                Pair("MB", "Manitoba"),
                Pair("NB", "New Brunswick"),
                Pair("NL", "Newfoundland and Labrador"),
                Pair("NT", "Northwest Territories"),
                Pair("NS", "Nova Scotia"),
                Pair("NU", "Nunavut"),
                Pair("ON", "Ontario"),
                Pair("PE", "Prince Edward Island"),
                Pair("QC", "Quebec"),
                Pair("SK", "Saskatchewan"),
                Pair("YT", "Yukon")
            )
        ) : Country(label, administrativeAreas)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class US(
            override val label: Int = CoreR.string.stripe_address_label_state,
            override val administrativeAreas: List<Pair<String, String>> = listOf(
                Pair("AL", "Alabama"),
                Pair("AK", "Alaska"),
                Pair("AS", "American Samoa"),
                Pair("AZ", "Arizona"),
                Pair("AR", "Arkansas"),
                Pair("AA", "Armed Forces (AA)"),
                Pair("AE", "Armed Forces (AE)"),
                Pair("AP", "Armed Forces (AP)"),
                Pair("CA", "California"),
                Pair("CO", "Colorado"),
                Pair("CT", "Connecticut"),
                Pair("DE", "Delaware"),
                Pair("DC", "District of Columbia"),
                Pair("FL", "Florida"),
                Pair("GA", "Georgia"),
                Pair("GU", "Guam"),
                Pair("HI", "Hawaii"),
                Pair("ID", "Idaho"),
                Pair("IL", "Illinois"),
                Pair("IN", "Indiana"),
                Pair("IA", "Iowa"),
                Pair("KS", "Kansas"),
                Pair("KY", "Kentucky"),
                Pair("LA", "Louisiana"),
                Pair("ME", "Maine"),
                Pair("MH", "Marshal Islands"),
                Pair("MD", "Maryland"),
                Pair("MA", "Massachusetts"),
                Pair("MI", "Michigan"),
                Pair("FM", "Micronesia"),
                Pair("MN", "Minnesota"),
                Pair("MS", "Mississippi"),
                Pair("MO", "Missouri"),
                Pair("MT", "Montana"),
                Pair("NE", "Nebraska"),
                Pair("NV", "Nevada"),
                Pair("NH", "New Hampshire"),
                Pair("NJ", "New Jersey"),
                Pair("NM", "New Mexico"),
                Pair("NY", "New York"),
                Pair("NC", "North Carolina"),
                Pair("ND", "North Dakota"),
                Pair("MP", "Northern Mariana Islands"),
                Pair("OH", "Ohio"),
                Pair("OK", "Oklahoma"),
                Pair("OR", "Oregon"),
                Pair("PW", "Palau"),
                Pair("PA", "Pennsylvania"),
                Pair("PR", "Puerto Rico"),
                Pair("RI", "Rhode Island"),
                Pair("SC", "South Carolina"),
                Pair("SD", "South Dakota"),
                Pair("TN", "Tennessee"),
                Pair("TX", "Texas"),
                Pair("UT", "Utah"),
                Pair("VT", "Vermont"),
                Pair("VI", "Virgin Islands"),
                Pair("VA", "Virginia"),
                Pair("WA", "Washington"),
                Pair("WV", "West Virginia"),
                Pair("WI", "Wisconsin"),
                Pair("WY", "Wyoming")
            )
        ) : Country(label, administrativeAreas)
    }
}

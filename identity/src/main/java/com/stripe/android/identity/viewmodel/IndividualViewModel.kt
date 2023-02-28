package com.stripe.android.identity.viewmodel

import androidx.lifecycle.ViewModel
import com.stripe.android.core.model.Country
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.RequiredInternationalAddress
import com.stripe.android.identity.ui.ADDRESS_SPEC
import com.stripe.android.identity.ui.EMPTY_ADDRESS_MAP
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.map

internal class IndividualViewModel(
    addressRepository: AddressRepository,
    addressCountries: List<Country>
) : ViewModel() {

    private val addressElement = AddressElement(
        _identifier = IdentifierSpec.Generic(ADDRESS_SPEC),
        addressRepository = addressRepository,
        countryCodes = addressCountries.map { it.code.value }.toSet(),
        rawValuesMap = EMPTY_ADDRESS_MAP,
        sameAsShippingElement = null,
        shippingValuesMap = null
    )

    val addressSection =
        SectionElement.wrap(
            addressElement, label = R.string.address_label_address
        )


    val currentAddress = addressElement.getFormFieldValueFlow().map {
        val addressMap = it.toMap()
        if (isValidAddress(addressMap)) {
            RequiredInternationalAddress(
                line1 = addressMap[IdentifierSpec.Line1]?.value!!,
                line2 = addressMap[IdentifierSpec.Line2]?.value.takeIf {
                    it?.isNotEmpty() == true
                },
                city = addressMap[IdentifierSpec.City]?.value!!,
                postalCode = addressMap[IdentifierSpec.PostalCode]?.value!!,
                state = addressMap[IdentifierSpec.State]?.value!!,
                country = addressMap[IdentifierSpec.Country]?.value!!,
            )
        } else {
            null
        }
    }

    private fun isValidAddress(addressMap: Map<IdentifierSpec, FormFieldEntry>): Boolean {
        if (listOf(
                IdentifierSpec.Line1,
                IdentifierSpec.City,
                IdentifierSpec.PostalCode,
                IdentifierSpec.State,
                IdentifierSpec.Country
            ).firstOrNull {
                addressMap[it]?.value.isNullOrBlank()
            } != null
        ) {
            return false
        }

        val country = requireNotNull(addressMap[IdentifierSpec.Country]?.value)
        val postal = requireNotNull(addressMap[IdentifierSpec.PostalCode]?.value)
        return when (val format = PostalCodeConfig.CountryPostalFormat.forCountry(country)) {
            is PostalCodeConfig.CountryPostalFormat.Other -> postal.isNotBlank()
            else -> {
                postal.length in format.minimumLength..format.maximumLength &&
                    postal.matches(format.regexPattern)
            }
        }
    }


}
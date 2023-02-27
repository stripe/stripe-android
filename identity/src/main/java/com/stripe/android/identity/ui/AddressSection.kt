package com.stripe.android.identity.ui

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import com.stripe.android.core.model.Country
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.CountryNotListedDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.RequiredInternationalAddress
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.forms.FormFieldEntry

/**
 * Section to collect User's Address.
 */
@Composable
internal fun AddressSection(
    identityViewModel: IdentityViewModel,
    addressCountries: List<Country>,
    addressNotListedText: String,
    navController: NavController,
    onAddressCollected: (Resource<RequiredInternationalAddress>) -> Unit
) {
    val addressElement = remember {
        AddressElement(
            _identifier = IdentifierSpec.Generic(ADDRESS_SPEC),
            addressRepository = identityViewModel.addressRepository,
            countryCodes = addressCountries.map { it.code.value }.toSet(),
            rawValuesMap = EMPTY_ADDRESS_MAP,
            sameAsShippingElement = null,
            shippingValuesMap = null
        )
    }

    val formFieldValue by addressElement.getFormFieldValueFlow().collectAsState(emptyList())

    val currentAddress: RequiredInternationalAddress? by remember {
        derivedStateOf {
            val addressMap = formFieldValue.toMap()
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
    }

    LaunchedEffect(currentAddress) {
        onAddressCollected(
            currentAddress?.let {
                Resource.success(it)
            } ?: run {
                Resource.loading()
            }
        )
    }
    AddressSectionContent(
        navController = navController,
        addressElement = addressElement,
        addressNotListedText = addressNotListedText
    )
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

@Composable
private fun AddressSectionContent(
    navController: NavController,
    addressElement: AddressElement,
    addressNotListedText: String
) {
    SectionElementUI(
        enabled = true,
        element = SectionElement.wrap(addressElement, label = R.string.address_label_address),
        hiddenIdentifiers = emptySet(),
        lastTextFieldIdentifier = null
    )
    TextButton(
        modifier = Modifier.testTag(ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG),
        onClick = {
            navController.navigateTo(
                CountryNotListedDestination(
                    isMissingId = false
                )
            )
        }
    ) {
        Text(text = addressNotListedText)
    }
}

internal const val ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG = "IdNumberSectionCountryNotListed"
internal const val ADDRESS_SPEC = "AddressSpec"

// Initial empty address provided to AddressElement to clean the cached values from global singleton.
private val EMPTY_ADDRESS_MAP = mapOf(
    IdentifierSpec.Line1 to "",
    IdentifierSpec.Line2 to "",
    IdentifierSpec.City to "",
    IdentifierSpec.PostalCode to "",
    IdentifierSpec.State to "",
    IdentifierSpec.Country to "",
)

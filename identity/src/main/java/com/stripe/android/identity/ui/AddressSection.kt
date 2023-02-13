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
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI

/**
 * Section to collect User's Address.
 */
@Composable
internal fun AddressSection(
    identityViewModel: IdentityViewModel,
    addressCountries: List<Country>,
    addressNotListedText: String,
    isStandalone: Boolean,
    navController: NavController,
    onAddressCollected: (Resource<RequiredInternationalAddress>) -> Unit
) {
    val addressElement = remember {
        AddressElement(
            _identifier = IdentifierSpec.Generic(ADDRESS_SPEC),
            addressRepository = identityViewModel.addressRepository,
            countryCodes = addressCountries.map { it.code.value }.toSet(),
            sameAsShippingElement = null,
            shippingValuesMap = null
        )
    }

    val formFieldValue by addressElement.getFormFieldValueFlow().collectAsState(emptyList())

    val currentAddress: RequiredInternationalAddress? by remember {
        derivedStateOf {
            val addressMap = formFieldValue.toMap()
            addressMap[IdentifierSpec.Line1]?.value?.takeIf { it.isNotEmpty() }?.let { line1 ->
                addressMap[IdentifierSpec.Country]?.value?.takeIf { it.isNotEmpty() }
                    ?.let { country ->
                        RequiredInternationalAddress(
                            line1 = line1,
                            line2 = addressMap[IdentifierSpec.Line2]?.value.takeIf {
                                it?.isNotEmpty() == true
                            },
                            city = addressMap[IdentifierSpec.City]?.value.takeIf {
                                it?.isNotEmpty() == true
                            },
                            postalCode = addressMap[IdentifierSpec.PostalCode]?.value.takeIf {
                                it?.isNotEmpty() == true
                            },
                            country = country,
                        )
                    }
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
                    isMissingId = false,
                    isFromStandaloneIndividual = isStandalone
                )
            )
        }
    ) {
        Text(text = addressNotListedText)
    }
}

internal const val ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG = "IdNumberSectionCountryNotListed"
internal const val ADDRESS_SPEC = "AddressSpec"

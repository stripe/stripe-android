package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.core.model.Country
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.CountryNotListedDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.RequiredInternationalAddress
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.address.transformToElementList
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldElementUI
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

/**
 * Section to collect User's Address.
 */
@Composable
internal fun AddressSection(
    enabled: Boolean,
    identityViewModel: IdentityViewModel,
    addressCountries: List<Country>,
    addressNotListedText: String,
    navController: NavController,
    onAddressCollected: (Resource<RequiredInternationalAddress>) -> Unit
) {
    val controller = remember {
        DropdownFieldController(
            CountryConfig(
                onlyShowCountryCodes = addressCountries.map { it.code.value }.toSet(),
                disableDropdownWithSingleElement = true
            )
        )
    }
    val selectedCountryCode by controller.rawFieldValue.collectAsState(addressCountries[0].code.value)
    val addressDetailSectionElements = remember(selectedCountryCode) {
        requireNotNull(
            identityViewModel.addressSchemaRepository
                .getSchema(selectedCountryCode)
        ).transformToElementList(
            requireNotNull(selectedCountryCode)
        )
    }
    val countryElement = remember { CountryElement(IdentifierSpec.Country, controller) }
    val sectionList = remember(selectedCountryCode) {
        mutableListOf<SectionFieldElement>(countryElement).also {
            it.addAll(addressDetailSectionElements)
        }
    }
    val sectionElement = remember(selectedCountryCode) {
        SectionElement.wrap(sectionList, R.string.address_label_address)
    }
    val formFieldValues by sectionElement.getFormFieldValueFlow()
        .collectAsState(initial = emptyList())
    val textIdentifiers by sectionElement.getTextFieldIdentifiers()
        .collectAsState(initial = emptyList())
    val currentAddress: RequiredInternationalAddress? by remember {
        derivedStateOf {
            val addressMap = formFieldValues.toMap()
            if (isValidAddress(addressMap)) {
                RequiredInternationalAddress(
                    line1 = addressMap[IdentifierSpec.Line1]?.value!!,
                    line2 = addressMap[IdentifierSpec.Line2]?.value.takeIf {
                        it?.isNotEmpty() == true
                    },
                    city = addressMap[IdentifierSpec.City]?.value!!,
                    postalCode = addressMap[IdentifierSpec.PostalCode]?.value!!,
                    state = addressMap[IdentifierSpec.State]?.value,
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
        enabled,
        addressNotListedText,
        sectionElement,
        textIdentifiers,
        navController
    )
}

@Composable
private fun AddressSectionContent(
    enabled: Boolean,
    addressNotListedText: String,
    sectionElement: SectionElement,
    textIdentifiers: List<IdentifierSpec>,
    navController: NavController
) {
    IdentityAddressSectionElementUI(
        enabled = enabled,
        element = sectionElement,
        hiddenIdentifiers = emptySet(),
        lastTextFieldIdentifier = textIdentifiers.lastOrNull()
    )

    TextButton(
        modifier = Modifier.testTag(ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG),
        contentPadding = PaddingValues(horizontal = 0.dp),
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

/**
 * Similar to SectionElementUI, but uses FocusDirection.Next and FocusDirection.Previous for
 * focus directions. The City field will be skipped with default values
 */
@Composable
private fun IdentityAddressSectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    if (!hiddenIdentifiers.contains(element.identifier)) {
        val controller = element.controller

        val error by controller.error.collectAsState(null)
        val sectionErrorString = error?.let {
            it.formatArgs?.let { args ->
                stringResource(
                    it.errorMessage,
                    *args
                )
            } ?: stringResource(it.errorMessage)
        }

        val elementsInsideCard = element.fields.filter {
            !it.shouldRenderOutsideCard
        }
        val elementsOutsideCard = element.fields.filter {
            it.shouldRenderOutsideCard
        }

        Section(
            controller.label,
            sectionErrorString,
            contentOutsideCard = {
                elementsOutsideCard.forEach { field ->
                    SectionFieldElementUI(
                        enabled,
                        field,
                        hiddenIdentifiers = hiddenIdentifiers,
                        lastTextFieldIdentifier = lastTextFieldIdentifier
                    )
                }
            },
            contentInCard = {
                elementsInsideCard.forEachIndexed { index, field ->
                    SectionFieldElementUI(
                        enabled,
                        field,
                        hiddenIdentifiers = hiddenIdentifiers,
                        lastTextFieldIdentifier = lastTextFieldIdentifier,
                        nextFocusDirection = FocusDirection.Next,
                        previousFocusDirection = FocusDirection.Previous
                    )
                    if (index != elementsInsideCard.lastIndex) {
                        Divider(
                            color = MaterialTheme.stripeColors.componentDivider,
                            thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.stripeShapes.borderStrokeWidth.dp
                            )
                        )
                    }
                }
            }
        )
    }
}

private fun isValidAddress(addressMap: Map<IdentifierSpec, FormFieldEntry>): Boolean {
    addressMap.forEach { (spec, entry) ->
        if (REQUIRED_FIELDS.contains(spec) && entry.value.isNullOrBlank()) {
            return false
        }
    }
    val country = addressMap[IdentifierSpec.Country]?.value
    val postal = addressMap[IdentifierSpec.PostalCode]?.value
    if (country == null || postal == null) {
        return false
    }
    return when (val format = PostalCodeConfig.CountryPostalFormat.forCountry(country)) {
        is PostalCodeConfig.CountryPostalFormat.Other -> postal.isNotBlank()
        else -> {
            postal.length in format.minimumLength..format.maximumLength &&
                postal.matches(format.regexPattern)
        }
    }
}

private val REQUIRED_FIELDS = listOf(
    IdentifierSpec.Line1,
    IdentifierSpec.City,
    IdentifierSpec.PostalCode,
    IdentifierSpec.State,
    IdentifierSpec.Country
)

internal const val ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG = "IdNumberSectionCountryNotListed"

package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldValidationController
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface BillingAddressCollectionMode {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Never : BillingAddressCollectionMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Full : BillingAddressCollectionMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Country(
        val additionalFieldsByCountry: Map<String, Set<IdentifierSpec>>,
    ) : BillingAddressCollectionMode
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun cardBillingAddressCollectionMode(
    addressCollectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode,
    requiresBillingAddressForAutomaticTax: Boolean,
): BillingAddressCollectionMode {
    return when (addressCollectionMode) {
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> BillingAddressCollectionMode.Never
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> BillingAddressCollectionMode.Full
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
            val additionalFieldsByCountry = buildMap {
                listOf("US", "GB", "CA").forEach { countryCode ->
                    put(countryCode, setOf(IdentifierSpec.PostalCode))
                }
                if (requiresBillingAddressForAutomaticTax) {
                    additionalAutomaticTaxFieldsByCountry.forEach { (countryCode, fields) ->
                        put(countryCode, get(countryCode).orEmpty() + fields)
                    }
                }
            }
            BillingAddressCollectionMode.Country(additionalFieldsByCountry)
        }
    }
}

/**
 * An address element that dynamically removes fields based on the selected country and collection mode.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BillingAddressElement(
    private val configuration: Configuration,
    private val countryDropdownFieldController: DropdownFieldController,
    sameAsShippingElement: SameAsShippingElement?,
) : AddressFieldsElement {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Configuration(
        val identifier: IdentifierSpec,
        val initialValues: Map<IdentifierSpec, String?>,
        val countryCodes: Set<String>,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        val shippingValues: Map<IdentifierSpec, String?>?,
        val addressCollectionMode: BillingAddressCollectionMode,
        val collectionConfiguration: BillingDetailsCollectionConfiguration,
        val shouldHideCountryOnNoAddressCollection: Boolean,
    )

    override val identifier: IdentifierSpec = configuration.identifier

    private val nameConfig = if (configuration.collectionConfiguration.collectName) {
        AddressFieldConfiguration.REQUIRED
    } else {
        AddressFieldConfiguration.HIDDEN
    }

    private val emailConfig = if (configuration.collectionConfiguration.collectEmail) {
        AddressFieldConfiguration.REQUIRED
    } else {
        AddressFieldConfiguration.HIDDEN
    }

    private val phoneNumberConfig = if (configuration.collectionConfiguration.collectPhone) {
        AddressFieldConfiguration.REQUIRED
    } else {
        AddressFieldConfiguration.HIDDEN
    }

    @VisibleForTesting
    val addressElement = configuration.autocompleteAddressInteractorFactory?.takeIf {
        configuration.addressCollectionMode == BillingAddressCollectionMode.Full
    }?.let { factory ->
        AutocompleteAddressElement(
            identifier = identifier,
            initialValues = configuration.initialValues,
            countryCodes = configuration.countryCodes,
            nameConfig = nameConfig,
            phoneNumberConfig = phoneNumberConfig,
            emailConfig = emailConfig,
            countryDropdownFieldController = countryDropdownFieldController,
            interactorFactory = factory,
            shippingValuesMap = configuration.shippingValues,
            sameAsShippingElement = sameAsShippingElement,
        )
    } ?: run {
        AddressElement(
            _identifier = identifier,
            rawValuesMap = configuration.initialValues,
            countryCodes = configuration.countryCodes,
            addressInputMode = AddressInputMode.NoAutocomplete(
                nameConfig = nameConfig,
                phoneNumberConfig = phoneNumberConfig,
                emailConfig = emailConfig,
            ),
            countryElement = CountryElement(
                identifier = IdentifierSpec.Country,
                controller = countryDropdownFieldController,
            ),
            shippingValuesMap = configuration.shippingValues,
            sameAsShippingElement = sameAsShippingElement,
            hideCountry = configuration.shouldHideCountryOnNoAddressCollection &&
                configuration.addressCollectionMode == BillingAddressCollectionMode.Never,
        )
    }

    private val addressElementSectionController = addressElement.sectionFieldErrorController()
    private val cardBillingAddressElementSectionErrorController =
        object : SectionFieldValidationController by addressElementSectionController, SectionFieldComposable {
            override val validationMessage: StateFlow<FieldValidationMessage?> =
                addressElement.addressController
                    .flatMapLatestAsStateFlow { addressController ->
                        combineAsStateFlow(
                            addressController.fieldsFlowable,
                            hiddenIdentifiers
                        ) { fields, hiddenIdentifiers ->
                            fields.flatMap { field ->
                                when (field) {
                                    is RowElement -> field.fields.filterNot { rowField ->
                                        hiddenIdentifiers.contains(rowField.identifier)
                                    }
                                    else -> listOf(field).takeUnless {
                                        hiddenIdentifiers.contains(field.identifier)
                                    } ?: emptyList()
                                }
                            }
                        }
                    }
                    .flatMapLatestAsStateFlow { fields ->
                        combineAsStateFlow(
                            fields.map { it.sectionFieldErrorController().validationMessage }
                        ) { fieldErrors ->
                            fieldErrors.filterNotNull().firstOrNull()
                        }
                    }

            @Composable
            override fun ComposeUI(
                enabled: Boolean,
                field: SectionFieldElement,
                modifier: Modifier,
                hiddenIdentifiers: Set<IdentifierSpec>,
                lastTextFieldIdentifier: IdentifierSpec?
            ) {
                if (addressElementSectionController is SectionFieldComposable) {
                    addressElementSectionController.ComposeUI(
                        enabled = enabled,
                        field = field,
                        modifier = modifier,
                        hiddenIdentifiers = hiddenIdentifiers,
                        lastTextFieldIdentifier = lastTextFieldIdentifier,
                    )
                }
            }
        }

    override val addressController: StateFlow<AddressController> = addressElement.addressController

    override val countryElement: CountryElement = addressElement.countryElement

    // Save for future use puts this in the controller rather than element
    // card and achv2 uses save for future use
    val hiddenIdentifiers: StateFlow<Set<IdentifierSpec>> =
        countryDropdownFieldController.rawFieldValue.mapAsStateFlow { countryCode ->
            when (val mode = configuration.addressCollectionMode) {
                BillingAddressCollectionMode.Never -> {
                    FieldType.entries
                        .filterNot { it == FieldType.Name }
                        .map { it.identifierSpec }
                        .toSet()
                }
                BillingAddressCollectionMode.Full -> {
                    emptySet()
                }
                is BillingAddressCollectionMode.Country -> {
                    val shownFields = mode.additionalFieldsByCountry[countryCode].orEmpty()
                    FieldType.entries
                        // Filtering name causes the field to be hidden even outside
                        // of this form.
                        .filterNot { it == FieldType.Name || it.identifierSpec in shownFields }
                        .map { it.identifierSpec }
                        .toSet()
                }
            }
        }

    override val allowsUserInteraction: Boolean = addressElement.allowsUserInteraction

    override val mandateText: ResolvableString? = addressElement.mandateText

    override fun getFormFieldValueFlow() = addressElement.getFormFieldValueFlow()

    override fun sectionFieldErrorController(): SectionFieldValidationController =
        cardBillingAddressElementSectionErrorController

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) = addressElement.setRawValue(rawValuesMap)

    override fun getTextFieldIdentifiers() = addressElement.getTextFieldIdentifiers()

    override fun onValidationStateChanged(isValidating: Boolean) {
        addressElement.onValidationStateChanged(isValidating)
    }

    fun withAdditionalFields(
        additionalFieldsByCountry: Map<String, Set<IdentifierSpec>>,
        sameAsShippingElement: SameAsShippingElement?,
    ): BillingAddressElement {
        val widenedCollectionMode = when (val mode = configuration.addressCollectionMode) {
            BillingAddressCollectionMode.Never -> mode
            BillingAddressCollectionMode.Full -> mode
            is BillingAddressCollectionMode.Country -> BillingAddressCollectionMode.Country(
                additionalFieldsByCountry = buildMap {
                    putAll(mode.additionalFieldsByCountry)
                    additionalFieldsByCountry.forEach { (countryCode, additionalFields) ->
                        put(countryCode, get(countryCode).orEmpty() + additionalFields)
                    }
                }
            )
        }

        return BillingAddressElement(
            configuration = configuration.copy(
                addressCollectionMode = widenedCollectionMode,
            ),
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = sameAsShippingElement,
        )
    }
}

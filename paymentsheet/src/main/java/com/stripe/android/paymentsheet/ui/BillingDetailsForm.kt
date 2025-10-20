package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Stable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

@Stable
internal class BillingDetailsForm(
    billingDetails: PaymentMethod.BillingDetails?,
    addressCollectionMode: AddressCollectionMode,
    private val nameCollection: NameCollection,
    private val collectEmail: Boolean,
    private val collectPhone: Boolean,
    allowedBillingCountries: Set<String>
) {
    val nameElement: SimpleTextElement? = if (nameCollection == NameCollection.OutsideBillingDetailsForm) {
        SimpleTextElement(
            identifier = IdentifierSpec.Name,
            controller = NameConfig.createController(billingDetails?.name)
        )
    } else {
        null
    }

    private val cardBillingAddressElement: CardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        countryCodes = allowedBillingCountries,
        collectionConfiguration = BillingDetailsCollectionConfiguration(
            address = when (addressCollectionMode) {
                AddressCollectionMode.Automatic ->
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
                AddressCollectionMode.Never -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                AddressCollectionMode.Full -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            collectName = nameCollection == NameCollection.InBillingDetailsForm,
            collectEmail = collectEmail,
            collectPhone = collectPhone,
        ),
        rawValuesMap = rawAddressValues(billingDetails),
        autocompleteAddressInteractorFactory = null,
        shouldHideCountryOnNoAddressCollection = false,
    )

    val addressSectionElement = SectionElement.wrap(
        sectionFieldElement = cardBillingAddressElement,
        label = resolvableString(R.string.stripe_billing_details),
    )
    val hiddenElements = cardBillingAddressElement.hiddenIdentifiers
    val formFieldsState = formFieldsState()

    private fun formFieldsState(): Flow<BillingDetailsFormState> {
        val nameFlow = nameElement?.getFormFieldValueFlow() ?: flowOf(emptyList())

        return combine(
            nameFlow,
            cardBillingAddressElement.getFormFieldValueFlow(),
            hiddenElements
        ) { nameFormFields, addressFormFields, hiddenIdentifiers ->
            val name = when (nameCollection) {
                NameCollection.InBillingDetailsForm ->
                    addressFormFields.valueOrNull(IdentifierSpec.Name, hiddenIdentifiers)
                NameCollection.OutsideBillingDetailsForm ->
                    nameFormFields.find { it.first == IdentifierSpec.Name }?.second
                NameCollection.Disabled -> null
            }

            val email = addressFormFields.valueOrNull(IdentifierSpec.Email, hiddenIdentifiers)
            val phone = addressFormFields.valueOrNull(IdentifierSpec.Phone, hiddenIdentifiers)
            val line1 = addressFormFields.valueOrNull(IdentifierSpec.Line1, hiddenIdentifiers)
            val line2 = addressFormFields.valueOrNull(IdentifierSpec.Line2, hiddenIdentifiers)
            val city = addressFormFields.valueOrNull(IdentifierSpec.City, hiddenIdentifiers)
            val postalCode = addressFormFields.valueOrNull(IdentifierSpec.PostalCode, hiddenIdentifiers)
            val country = addressFormFields.valueOrNull(IdentifierSpec.Country, hiddenIdentifiers)
            val state = addressFormFields.valueOrNull(IdentifierSpec.State, hiddenIdentifiers)
            BillingDetailsFormState(
                name = name,
                email = email,
                phone = phone,
                line1 = line1,
                line2 = line2,
                city = city,
                postalCode = postalCode,
                country = country,
                state = state,
            )
        }.flowOn(Dispatchers.Main)
    }

    private fun List<Pair<IdentifierSpec, FormFieldEntry>>.valueOrNull(
        identifierSpec: IdentifierSpec,
        hiddenIdentifiers: Set<IdentifierSpec>
    ): FormFieldEntry? {
        if (hiddenIdentifiers.contains(identifierSpec)) return null
        return firstOrNull {
            it.first == identifierSpec
        }?.second
    }

    private fun rawAddressValues(
        billingDetails: PaymentMethod.BillingDetails?,
    ): Map<IdentifierSpec, String?> {
        val address = billingDetails?.address

        return listOfNotNull(
            (IdentifierSpec.Name to billingDetails?.name).takeIf {
                nameCollection == NameCollection.InBillingDetailsForm
            },
            IdentifierSpec.Line1 to address?.line1,
            IdentifierSpec.Line2 to address?.line2,
            IdentifierSpec.State to address?.state,
            IdentifierSpec.City to address?.city,
            IdentifierSpec.Country to address?.country,
            IdentifierSpec.PostalCode to address?.postalCode,
            (IdentifierSpec.Email to billingDetails?.email).takeIf {
                collectEmail
            },
            (IdentifierSpec.Phone to billingDetails?.phone).takeIf {
                collectPhone
            },
        ).toMap()
    }
}

internal enum class NameCollection {
    Disabled,
    InBillingDetailsForm,
    OutsideBillingDetailsForm,
}

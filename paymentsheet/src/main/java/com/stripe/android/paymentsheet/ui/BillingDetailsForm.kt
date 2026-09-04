package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Stable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.cardBillingAddressCollectionMode
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormFieldId
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
    allowedBillingCountries: Set<String>,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
) {
    val nameElement: SimpleTextElement? = if (nameCollection == NameCollection.OutsideBillingDetailsForm) {
        SimpleTextElement(
            identifier = FormFieldId.Name,
            controller = NameConfig.createController(billingDetails?.name)
        )
    } else {
        null
    }

    private val cardBillingAddressElement: BillingAddressElement = BillingAddressElement(
        identifier = FormFieldId.BillingAddress,
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
        addressCollectionMode = cardBillingAddressCollectionMode(
            addressCollectionMode = when (addressCollectionMode) {
                AddressCollectionMode.Automatic ->
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
                AddressCollectionMode.Never -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                AddressCollectionMode.Full -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            requiresBillingAddressForAutomaticTax = false,
        ),
        rawValuesMap = rawAddressValues(billingDetails),
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
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
                    addressFormFields.valueOrNull(FormFieldId.Name, hiddenIdentifiers)
                NameCollection.OutsideBillingDetailsForm ->
                    nameFormFields.find { it.first == FormFieldId.Name }?.second
                NameCollection.Disabled -> null
            }

            val email = addressFormFields.valueOrNull(FormFieldId.Email, hiddenIdentifiers)
            val phone = addressFormFields.valueOrNull(FormFieldId.Phone, hiddenIdentifiers)
            val line1 = addressFormFields.valueOrNull(FormFieldId.Line1, hiddenIdentifiers)
            val line2 = addressFormFields.valueOrNull(FormFieldId.Line2, hiddenIdentifiers)
            val city = addressFormFields.valueOrNull(FormFieldId.City, hiddenIdentifiers)
            val postalCode = addressFormFields.valueOrNull(FormFieldId.PostalCode, hiddenIdentifiers)
            val country = addressFormFields.valueOrNull(FormFieldId.Country, hiddenIdentifiers)
            val state = addressFormFields.valueOrNull(FormFieldId.State, hiddenIdentifiers)
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

    private fun List<Pair<FormFieldId, FormFieldEntry>>.valueOrNull(
        formFieldId: FormFieldId,
        hiddenIdentifiers: Set<FormFieldId>
    ): FormFieldEntry? {
        if (hiddenIdentifiers.contains(formFieldId)) return null
        return firstOrNull {
            it.first == formFieldId
        }?.second
    }

    private fun rawAddressValues(
        billingDetails: PaymentMethod.BillingDetails?,
    ): Map<FormFieldId, String?> {
        val address = billingDetails?.address

        return listOfNotNull(
            (FormFieldId.Name to billingDetails?.name).takeIf {
                nameCollection == NameCollection.InBillingDetailsForm
            },
            FormFieldId.Line1 to address?.line1,
            FormFieldId.Line2 to address?.line2,
            FormFieldId.State to address?.state,
            FormFieldId.City to address?.city,
            FormFieldId.Country to address?.country,
            FormFieldId.PostalCode to address?.postalCode,
            (FormFieldId.Email to billingDetails?.email).takeIf {
                collectEmail
            },
            (FormFieldId.Phone to billingDetails?.phone).takeIf {
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

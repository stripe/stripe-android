package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Stable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.definitions.toInternal
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
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
    collectName: Boolean,
    collectEmail: Boolean,
    collectPhone: Boolean,
) {

    val nameElement: SimpleTextElement? = if (collectName) {
        SimpleTextElement(
            identifier = IdentifierSpec.Name,
            controller = NameConfig.createController(billingDetails?.name)
        )
    } else {
        null
    }

    val emailElement: EmailElement? = if (collectEmail) {
        EmailElement(initialValue = billingDetails?.email)
    } else {
        null
    }

    val phoneElement: PhoneNumberElement? = if (collectPhone) {
        PhoneNumberElement(
            identifier = IdentifierSpec.Phone,
            controller = PhoneNumberController.createPhoneNumberController(
                initialValue = billingDetails?.phone ?: "",
            )
        )
    } else {
        null
    }

    private val cardBillingAddressElement: CardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        collectionMode = addressCollectionMode.toInternal(),
        rawValuesMap = rawAddressValues(billingDetails),
        autocompleteAddressInteractorFactory = null,
    )

    val addressSectionElement = SectionElement.wrap(
        sectionFieldElement = cardBillingAddressElement,
        label = resolvableString(R.string.stripe_billing_details),
    )
    val hiddenElements = cardBillingAddressElement.hiddenIdentifiers
    val formFieldsState = formFieldsState()

    private fun formFieldsState(): Flow<BillingDetailsFormState> {
        val nameFlow = nameElement?.getFormFieldValueFlow() ?: flowOf(emptyList())
        val emailFlow = emailElement?.getFormFieldValueFlow() ?: flowOf(emptyList())
        val phoneFlow = phoneElement?.getFormFieldValueFlow() ?: flowOf(emptyList())

        return combine(
            nameFlow,
            emailFlow,
            phoneFlow,
            cardBillingAddressElement.getFormFieldValueFlow(),
            hiddenElements
        ) { nameFormFields, emailFormFields, phoneFormFields, addressFormFields, hiddenIdentifiers ->
            val name = nameFormFields.find { it.first == IdentifierSpec.Name }?.second
            val email = emailFormFields.find { it.first == IdentifierSpec.Email }?.second
            val phone = phoneFormFields.find { it.first == IdentifierSpec.Phone }?.second
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
        val address = billingDetails?.address ?: return emptyMap()
        return mapOf(
            IdentifierSpec.Line1 to address.line1,
            IdentifierSpec.Line2 to address.line2,
            IdentifierSpec.State to address.state,
            IdentifierSpec.City to address.city,
            IdentifierSpec.Country to address.country,
            IdentifierSpec.PostalCode to address.postalCode
        )
    }
}

package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Stable
import com.stripe.android.lpmfoundations.paymentmethod.definitions.toInternal
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

@Stable
internal class BillingDetailsForm(
    billingDetails: PaymentMethod.BillingDetails?,
    addressCollectionMode: AddressCollectionMode,
) {

    private val cardBillingAddressElement: CardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        collectionMode = addressCollectionMode.toInternal(),
        rawValuesMap = rawAddressValues(billingDetails)
    )

    val addressSectionElement = SectionElement.wrap(
        sectionFieldElement = cardBillingAddressElement,
        label = R.string.stripe_billing_details,
    )
    val hiddenElements = cardBillingAddressElement.hiddenIdentifiers
    val formFieldsState = formFieldsState()

    private fun formFieldsState(): Flow<BillingDetailsFormState> {
        return combine(
            flow = cardBillingAddressElement.getFormFieldValueFlow(),
            flow2 = hiddenElements
        ) { fieldList, hiddenIdentifiers ->
            val line1 = fieldList.valueOrNull(IdentifierSpec.Line1, hiddenIdentifiers)
            val line2 = fieldList.valueOrNull(IdentifierSpec.Line2, hiddenIdentifiers)
            val city = fieldList.valueOrNull(IdentifierSpec.City, hiddenIdentifiers)
            val postalCode = fieldList.valueOrNull(IdentifierSpec.PostalCode, hiddenIdentifiers)
            val country = fieldList.valueOrNull(IdentifierSpec.Country, hiddenIdentifiers)
            val state = fieldList.valueOrNull(IdentifierSpec.State, hiddenIdentifiers)
            BillingDetailsFormState(
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

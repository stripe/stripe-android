package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Stable
import com.stripe.android.lpmfoundations.paymentmethod.definitions.toInternal
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

@Stable
internal class BillingAddressForm(
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
) {

    private val cardBillingAddressElement: CardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        collectionMode = addressCollectionMode.toInternal(),
        rawValuesMap = rawAddressValues(billingDetails)
    )

    val addressSectionElement = SectionElement.wrap(cardBillingAddressElement)
    val hiddenElements = cardBillingAddressElement.hiddenIdentifiers
    val formFieldsState = formFieldsState()

    private fun formFieldsState(): Flow<BillingDetailsFormState> {
        return combine(
            flow = cardBillingAddressElement.getFormFieldValueFlow(),
            flow2 = hiddenElements
        ) { fieldList, hiddenIdentifiers ->
            // What is the IdentifierSpec.OneLineAddress?
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
                billingDetails = billingDetails,
                addressCollectionMode = addressCollectionMode
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

internal data class BillingDetailsFormState(
    val line1: FormFieldEntry?,
    val line2: FormFieldEntry?,
    val city: FormFieldEntry?,
    val postalCode: FormFieldEntry?,
    val state: FormFieldEntry?,
    val country: FormFieldEntry?,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
) {
    fun hasChanged(): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                billingDetails?.address?.postalCode != postalCode?.value ||
                    billingDetails?.address?.country != country?.value
            }
            AddressCollectionMode.Never -> false
            AddressCollectionMode.Full -> {
                billingDetails?.address?.postalCode != postalCode?.value ||
                    billingDetails?.address?.country != country?.value ||
                    billingDetails?.address?.line1 != line1?.value ||
                    billingDetails?.address?.line2 != line2?.value ||
                    billingDetails?.address?.city != city?.value ||
                    billingDetails?.address?.state != state?.value
            }
        }
    }

    fun isComplete(): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                country.isValid && postalCode.isValid
            }
            AddressCollectionMode.Never -> {
                true
            }
            AddressCollectionMode.Full -> {
                country.isValid && state.isValid && postalCode.isValid && line1.isValid && line2.isValid && city.isValid
            }
        }
    }

    private val FormFieldEntry?.isValid
        get() = this?.isComplete ?: true
}

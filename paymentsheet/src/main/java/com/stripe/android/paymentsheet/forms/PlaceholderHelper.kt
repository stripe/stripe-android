package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal object PlaceholderHelper {
    /**
     * Returns the list of specs by adding or removing billing details fields.
     */
    internal fun specsForConfiguration(
        specs: List<FormItemSpec>,
        requiresMandate: Boolean,
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): List<FormItemSpec> {
        val billingDetailsPlaceholders = mutableListOf(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        val modifiedSpecs = specs.mapNotNull {
            removeCorrespondingPlaceholder(billingDetailsPlaceholders, it)
            when (it) {
                is NameSpec -> it.takeUnless {
                    configuration.name == CollectionMode.Never
                }
                is EmailSpec -> it.takeUnless {
                    configuration.email == CollectionMode.Never
                }
                is PhoneSpec -> it.takeUnless {
                    configuration.phone == CollectionMode.Never
                }
                is AddressSpec -> it.takeUnless {
                    configuration.address == AddressCollectionMode.Never
                }
                is SepaMandateTextSpec -> it.takeIf {
                    requiresMandate
                }
                is PlaceholderSpec -> specForPlaceholderField(it.field, requiresMandate, configuration)
                else -> it
            }
        }.plus(
            // Add additional fields that don't have a placeholder, if necessary.
            billingDetailsPlaceholders.mapNotNull { specForPlaceholderField(it, requiresMandate, configuration) }
        )

        return modifiedSpecs
    }

    @VisibleForTesting
    internal fun removeCorrespondingPlaceholder(
        placeholderFields: MutableList<PlaceholderField>,
        spec: FormItemSpec
    ) {
        when (spec) {
            is NameSpec -> placeholderFields.remove(PlaceholderSpec.PlaceholderField.Name)
            is EmailSpec -> placeholderFields.remove(PlaceholderSpec.PlaceholderField.Email)
            is PhoneSpec -> placeholderFields.remove(PlaceholderSpec.PlaceholderField.Phone)
            is AddressSpec ->
                placeholderFields.remove(PlaceholderSpec.PlaceholderField.BillingAddress)
            is SepaMandateTextSpec -> placeholderFields.remove(PlaceholderField.SepaMandate)
            is PlaceholderSpec -> when (spec.field) {
                PlaceholderSpec.PlaceholderField.BillingAddressWithoutCountry ->
                    placeholderFields.remove(PlaceholderSpec.PlaceholderField.BillingAddress)
                else -> placeholderFields.remove(spec.field)
            }
            else -> Unit
        }
    }

    @VisibleForTesting
    internal fun specForPlaceholderField(
        field: PlaceholderField,
        requiresMandate: Boolean,
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ) = when (field) {
        PlaceholderField.Name -> NameSpec().takeIf {
            configuration.name == CollectionMode.Always
        }
        PlaceholderField.Email -> EmailSpec().takeIf {
            configuration.email == CollectionMode.Always
        }
        PlaceholderField.Phone -> PhoneSpec().takeIf {
            configuration.phone == CollectionMode.Always
        }
        PlaceholderField.BillingAddress -> AddressSpec().takeIf {
            configuration.address == AddressCollectionMode.Full
        }
        PlaceholderSpec.PlaceholderField.BillingAddressWithoutCountry ->
            AddressSpec(hideCountry = true).takeIf {
                configuration.address == AddressCollectionMode.Full
            }
        PlaceholderField.SepaMandate -> SepaMandateTextSpec().takeIf {
            requiresMandate
        }
        else -> null
    }

    internal suspend fun connectBillingDetailsFields(elementsFlow: Flow<List<FormElement>>) {
        var phoneNumberElement: PhoneNumberElement? = null

        elementsFlow.map { elementsList ->
            elementsList
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<PhoneNumberElement>()
                .firstOrNull()
        }.collect {
            phoneNumberElement = it
        }

        elementsFlow
            .flatMapConcat { elementsList ->
                // Look for a standalone CountryElement.
                // Note that this should be done first, because AddressElement always has a
                // CountryElement, but it might be hidden.
                var countryElement = elementsList
                    .filterIsInstance<SectionElement>()
                    .flatMap { it.fields }
                    .filterIsInstance<CountryElement>()
                    .firstOrNull()

                // If not found, look for one inside an AddressElement.
                if (countryElement == null) {
                    countryElement = elementsList
                        .filterIsInstance<SectionElement>()
                        .flatMap { it.fields }
                        .filterIsInstance<AddressElement>()
                        .firstOrNull()
                        ?.countryElement
                }

                countryElement?.controller?.rawFieldValue ?: emptyFlow()
            }
            .filterNotNull()
            .collect {
                if (phoneNumberElement?.controller?.getLocalNumber().isNullOrBlank()) {
                    phoneNumberElement?.controller?.countryDropdownController?.onRawValueChange(it)
                }
            }
    }
}

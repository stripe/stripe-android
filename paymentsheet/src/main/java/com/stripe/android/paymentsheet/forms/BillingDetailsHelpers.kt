package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField

internal class BillingDetailsHelpers {
    companion object {
        /**
         * Returns the list of specs by adding or removing billing details fields.
         */
        internal fun specsForConfiguration(
            specs: List<FormItemSpec>,
            configuration: BillingDetailsCollectionConfiguration,
        ): List<FormItemSpec> {
            var billingDetailsPlaceholders = mutableListOf(
                PlaceholderField.Name,
                PlaceholderField.Email,
                PlaceholderField.Phone,
                PlaceholderField.BillingAddress,
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
                    is PlaceholderSpec -> specForPlaceholderField(it.field, configuration)
                    else -> it
                }
            }.plus(
                // Add additional fields that don't have a placeholder, if necessary.
                billingDetailsPlaceholders.mapNotNull { specForPlaceholderField(it, configuration) }
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
            configuration: BillingDetailsCollectionConfiguration,
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
            else -> null
        }
    }
}

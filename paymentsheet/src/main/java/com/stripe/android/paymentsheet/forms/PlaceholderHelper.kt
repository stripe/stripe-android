package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object PlaceholderHelper {
    /**
     * Returns the list of specs by adding or removing billing details fields.
     */
    internal fun specsForConfiguration(
        specs: List<FormItemSpec>,
        placeholderOverrideList: List<IdentifierSpec>,
        requiresMandate: Boolean,
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration,
        termsDisplay: PaymentSheet.TermsDisplay,
    ): List<FormItemSpec> {
        val billingDetailsPlaceholders = mutableListOf(
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

                is PlaceholderSpec -> specForPlaceholderField(
                    field = it.field,
                    placeholderOverrideList = placeholderOverrideList,
                    requiresMandate = requiresMandate,
                    configuration = configuration,
                    termsDisplay = termsDisplay,
                )

                is SepaMandateTextSpec -> it.takeUnless {
                    termsDisplay == PaymentSheet.TermsDisplay.NEVER
                }

                else -> it
            }
        }.plus(
            // Add additional fields that don't have a placeholder, if necessary.
            billingDetailsPlaceholders.mapNotNull {
                specForPlaceholderField(
                    field = it,
                    placeholderOverrideList = placeholderOverrideList,
                    requiresMandate = requiresMandate,
                    configuration = configuration,
                    termsDisplay = termsDisplay,
                )
            }
        )

        return modifiedSpecs.sortedWith { o1, o2 ->
            if (o1 is MandateTextSpec || o1 is CashAppPayMandateTextSpec) {
                1
            } else if (o2 is MandateTextSpec || o2 is CashAppPayMandateTextSpec) {
                -1
            } else {
                0
            }
        }
    }

    @VisibleForTesting
    internal fun removeCorrespondingPlaceholder(
        placeholderFields: MutableList<PlaceholderField>,
        spec: FormItemSpec
    ) {
        when (spec) {
            is NameSpec -> placeholderFields.remove(PlaceholderField.Name)
            is EmailSpec -> placeholderFields.remove(PlaceholderField.Email)
            is PhoneSpec -> placeholderFields.remove(PlaceholderField.Phone)
            is AddressSpec ->
                placeholderFields.remove(PlaceholderField.BillingAddress)

            is SepaMandateTextSpec -> placeholderFields.remove(PlaceholderField.SepaMandate)
            is PlaceholderSpec -> when (spec.field) {
                PlaceholderField.BillingAddressWithoutCountry ->
                    placeholderFields.remove(PlaceholderField.BillingAddress)

                else -> placeholderFields.remove(spec.field)
            }

            else -> Unit
        }
    }

    @VisibleForTesting
    internal fun specForPlaceholderField(
        field: PlaceholderField,
        placeholderOverrideList: List<IdentifierSpec>,
        requiresMandate: Boolean,
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration,
        termsDisplay: PaymentSheet.TermsDisplay,
    ) = when (field) {
        PlaceholderField.Name -> NameSpec().takeIf {
            configuration.name == CollectionMode.Always ||
                (
                    placeholderOverrideList.contains(it.apiPath) &&
                        configuration.name != CollectionMode.Never
                    )
        }

        PlaceholderField.Email -> EmailSpec().takeIf {
            configuration.email == CollectionMode.Always ||
                (
                    placeholderOverrideList.contains(it.apiPath) &&
                        configuration.email != CollectionMode.Never
                    )
        }

        PlaceholderField.Phone -> PhoneSpec().takeIf {
            configuration.phone == CollectionMode.Always ||
                (
                    placeholderOverrideList.contains(it.apiPath) &&
                        configuration.phone != CollectionMode.Never
                    )
        }

        PlaceholderField.BillingAddress -> AddressSpec(
            allowedCountryCodes = configuration.allowedBillingCountries,
        ).takeIf {
            configuration.address == AddressCollectionMode.Full ||
                (
                    placeholderOverrideList.contains(it.apiPath) &&
                        configuration.address != AddressCollectionMode.Never
                    )
        }

        PlaceholderField.BillingAddressWithoutCountry ->
            AddressSpec(allowedCountryCodes = configuration.allowedBillingCountries, hideCountry = true).takeIf {
                configuration.address == AddressCollectionMode.Full ||
                    (
                        placeholderOverrideList.contains(it.apiPath) &&
                            configuration.address != AddressCollectionMode.Never
                        )
            }

        PlaceholderField.SepaMandate -> SepaMandateTextSpec().takeIf {
            requiresMandate && termsDisplay != PaymentSheet.TermsDisplay.NEVER
        }

        else -> null
    }
}

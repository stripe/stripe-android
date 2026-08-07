package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.KlarnaMandateTextSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal sealed interface ResolvedFormItem {
    data class ExistingSpec(
        val spec: FormItemSpec,
    ) : ResolvedFormItem

    data class BillingAddressSpec(
        val allowedCountryCodes: Set<String>,
    ) : ResolvedFormItem
}

internal object AutomaticTaxBillingAddressResolver {
    fun resolve(
        specs: List<FormItemSpec>,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresBillingAddressForAutomaticTax: Boolean,
        allowedBillingCountries: Set<String>,
    ): List<ResolvedFormItem> {
        val shouldCollectTaxBillingAddress =
            addressCollectionMode ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
                requiresBillingAddressForAutomaticTax

        if (!shouldCollectTaxBillingAddress) {
            return specs.map(ResolvedFormItem::ExistingSpec)
        }

        if (specs.any { it is AddressSpec }) {
            return specs.mapNotNull { spec ->
                when {
                    spec is CountrySpec && spec.apiPath == IdentifierSpec.Country -> null
                    spec is AddressSpec && spec.hideCountry -> {
                        ResolvedFormItem.ExistingSpec(spec.copy(hideCountry = false))
                    }
                    else -> ResolvedFormItem.ExistingSpec(spec)
                }
            }
        }

        val countrySpecIndex = specs.indexOfFirst { spec ->
            spec is CountrySpec && spec.apiPath == IdentifierSpec.Country
        }
        if (countrySpecIndex >= 0) {
            val countrySpec = specs[countrySpecIndex] as CountrySpec
            return specs.mapIndexed { index, spec ->
                if (index == countrySpecIndex) {
                    ResolvedFormItem.BillingAddressSpec(
                        allowedCountryCodes = countrySpec.allowedCountryCodes,
                    )
                } else {
                    ResolvedFormItem.ExistingSpec(spec)
                }
            }
        }

        val resolvedSpecs: MutableList<ResolvedFormItem> = specs
            .map(ResolvedFormItem::ExistingSpec)
            .toMutableList()
        val firstMandateIndex = specs.indexOfFirst(FormItemSpec::isMandate)
            .takeIf { it >= 0 }
            ?: specs.size
        resolvedSpecs.add(
            firstMandateIndex,
            ResolvedFormItem.BillingAddressSpec(
                allowedCountryCodes = allowedBillingCountries,
            )
        )
        return resolvedSpecs
    }
}

private fun FormItemSpec.isMandate(): Boolean {
    return this is AuBecsDebitMandateTextSpec ||
        this is CashAppPayMandateTextSpec ||
        this is KlarnaMandateTextSpec ||
        this is MandateTextSpec ||
        this is SepaMandateTextSpec
}

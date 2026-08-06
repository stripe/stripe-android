package com.stripe.android.lpmfoundations

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal fun assertCountryOnlyBillingAddressSection(
    section: SectionElement,
    countryIdentifier: IdentifierSpec,
): BillingAddressElement {
    assertThat(section.identifier.v1).isEqualTo("${countryIdentifier.v1}_section")
    assertThat(section.fields).hasSize(1)

    val billingAddressElement = section.fields.single() as BillingAddressElement
    assertThat(billingAddressElement.identifier).isEqualTo(countryIdentifier)
    assertThat(billingAddressElement.countryElement.identifier).isEqualTo(countryIdentifier)
    assertThat(billingAddressElement.hiddenIdentifiers.value).containsAtLeast(
        IdentifierSpec.Line1,
        IdentifierSpec.City,
        IdentifierSpec.State,
        IdentifierSpec.PostalCode,
    )

    return billingAddressElement
}

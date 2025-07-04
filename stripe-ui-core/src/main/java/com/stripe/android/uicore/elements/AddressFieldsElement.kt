package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AddressFieldsElement : SectionFieldElement {
    val countryElement: CountryElement
}

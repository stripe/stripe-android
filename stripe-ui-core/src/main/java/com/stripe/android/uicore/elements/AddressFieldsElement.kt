package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AddressFieldsElement : SectionFieldElement {
    val addressController: StateFlow<AddressController>
    val countryElement: CountryElement
}

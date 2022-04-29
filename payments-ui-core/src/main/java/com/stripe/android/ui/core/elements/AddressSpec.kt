package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class AddressSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),
    val countryCodes: Set<String>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressFieldElementRepository
    ) = createSectionElement(
        AddressElement(
            identifier,
            addressRepository,
            initialValues,
            countryCodes = countryCodes
        ),
        label = R.string.billing_details
    )
}

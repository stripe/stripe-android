package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@SerialName("billing_address")
data class AddressSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),
    val countryCodes: Set<String>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressFieldElementRepository
    ) = createSectionElement(
        AddressElement(
            api_path,
            addressRepository,
            initialValues,
            countryCodes = countryCodes
        ),
        label = R.string.billing_details
    )
}

package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
data class OTPSpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(): OTPElement {
        return OTPElement(
            identifier = identifier,
            controller = OTPController()
        )
    }
}
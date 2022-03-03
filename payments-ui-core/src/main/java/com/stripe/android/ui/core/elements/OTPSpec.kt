package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
object OTPSpec : FormItemSpec(), RequiredItemSpec {
    override val identifier: IdentifierSpec
        get() = IdentifierSpec.Generic("otp")
    fun transform(): OTPElement {
        return OTPElement(
            identifier = identifier,
            controller = OTPController()
        )
    }
}

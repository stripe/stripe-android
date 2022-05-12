package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
object OTPSpec : FormItemSpec(), RequiredItemSpec {
    override val api_path: IdentifierSpec
        get() = IdentifierSpec.Generic("otp")
    fun transform(): OTPElement {
        return OTPElement(
            identifier = api_path,
            controller = OTPController()
        )
    }
}

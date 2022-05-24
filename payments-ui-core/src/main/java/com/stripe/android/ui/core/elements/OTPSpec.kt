package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
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

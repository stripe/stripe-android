package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data object OTPSpec : FormItemSpec() {
    override val apiPath: IdentifierSpec
        get() = IdentifierSpec.Generic("otp")

    fun transform(): OTPElement {
        return OTPElement(
            identifier = apiPath,
            controller = OTPController()
        )
    }
}

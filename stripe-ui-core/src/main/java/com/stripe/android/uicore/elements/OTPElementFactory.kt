package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object OTPElementFactory {
    fun create(): OTPElement {
        return OTPElement(
            identifier = FormFieldId.Generic("otp"),
            controller = OTPController(),
        )
    }
}

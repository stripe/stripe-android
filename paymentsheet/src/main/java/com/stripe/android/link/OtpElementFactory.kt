package com.stripe.android.link

import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement

internal fun createOtpElement(): OTPElement {
    return OTPElement(
        identifier = IdentifierSpec.Generic("otp"),
        controller = OTPController(),
    )
}

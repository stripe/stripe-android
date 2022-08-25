package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ConsumerSignUpConsentAction(val value: String) {
    Checkbox("checkbox_mobile"), Button("button_mobile")
}

package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ConsumerSignUpConsentAction(val value: String) {
    Checkbox("clicked_checkbox_mobile"), Button("clicked_button_mobile")
}

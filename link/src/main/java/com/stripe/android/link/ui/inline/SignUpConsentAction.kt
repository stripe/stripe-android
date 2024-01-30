package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class SignUpConsentAction {
    Checkbox,
    CheckboxWithPrefilledEmail,
    CheckboxWithPrefilledEmailAndPhone,
    Implied,
    ImpliedWithPrefilledEmail
}

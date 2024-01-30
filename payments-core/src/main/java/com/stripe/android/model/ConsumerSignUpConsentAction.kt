package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ConsumerSignUpConsentAction(val value: String) {
    Checkbox("clicked_checkbox_no_spm_mobile_v0"),
    CheckboxWithPrefilledEmail("clicked_checkbox_nospm_mobile_v0_0"),
    CheckboxWithPrefilledEmailAndPhone("clicked_checkbox_nospm_mobile_v0_1"),
    Implied("implied_consent_withspm_mobile_v0"),
    ImpliedWithPrefilledEmail("implied_consent_withspm_mobile_v0_0")
}

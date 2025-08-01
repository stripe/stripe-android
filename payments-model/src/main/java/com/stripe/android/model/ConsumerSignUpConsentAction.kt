package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ConsumerSignUpConsentAction(val value: String) {
    // Payments
    Checkbox("clicked_checkbox_nospm_mobile_v0"),
    CheckboxWithPrefilledEmail("clicked_checkbox_nospm_mobile_v0_0"),
    CheckboxWithPrefilledEmailAndPhone("clicked_checkbox_nospm_mobile_v0_1"),
    Implied("implied_consent_withspm_mobile_v0"),
    ImpliedWithPrefilledEmail("implied_consent_withspm_mobile_v0_0"),
    PrecheckedOptInBoxPrefilledAll("prechecked_opt_in_box_prefilled_all"),
    PrecheckedOptInBoxPrefilledSome("prechecked_opt_in_box_prefilled_some"),
    PrecheckedOptInBoxPrefilledNone("prechecked_opt_in_box_prefilled_none"),
    SignUpOptInMobileChecked("sign_up_opt_in_mobile_checked"),

    // Financial Connections
    EnteredPhoneNumberClickedSaveToLink("entered_phone_number_clicked_save_to_link"),
}

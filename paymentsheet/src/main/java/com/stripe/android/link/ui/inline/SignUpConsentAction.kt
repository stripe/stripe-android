package com.stripe.android.link.ui.inline

import com.stripe.android.link.LinkController

internal enum class SignUpConsentAction {
    Checkbox,
    CheckboxWithPrefilledEmail,
    CheckboxWithPrefilledEmailAndPhone,
    Implied,
    ImpliedWithPrefilledEmail,
    DefaultOptInWithAllPrefilled,
    DefaultOptInWithSomePrefilled,
    DefaultOptInWithNonePrefilled,
    SignUpOptInMobileChecked,
    SignUpOptInMobilePrechecked,
    EnteredPhoneNumberEmailClickedSaveWithLinkIdentity,
}

internal fun LinkController.RegisterConsumerConsentAction.toSignUpConsentAction(): SignUpConsentAction =
    when (this) {
        LinkController.RegisterConsumerConsentAction.Implied -> SignUpConsentAction.Implied
        LinkController.RegisterConsumerConsentAction.NetworkedIdentity ->
            SignUpConsentAction.EnteredPhoneNumberEmailClickedSaveWithLinkIdentity
    }

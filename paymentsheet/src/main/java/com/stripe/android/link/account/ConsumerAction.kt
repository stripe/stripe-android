package com.stripe.android.link.account

import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSignUpConsentAction

internal val SignUpConsentAction.consumerAction: ConsumerSignUpConsentAction
    get() = when (this) {
        SignUpConsentAction.Checkbox ->
            ConsumerSignUpConsentAction.Checkbox
        SignUpConsentAction.CheckboxWithPrefilledEmail ->
            ConsumerSignUpConsentAction.CheckboxWithPrefilledEmail
        SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone ->
            ConsumerSignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
        SignUpConsentAction.Implied ->
            ConsumerSignUpConsentAction.Implied
        SignUpConsentAction.ImpliedWithPrefilledEmail ->
            ConsumerSignUpConsentAction.ImpliedWithPrefilledEmail
    }

package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.LayoutSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val USBankAccountParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "us_bank_account"
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val USBankAccountForm = LayoutSpec.create()

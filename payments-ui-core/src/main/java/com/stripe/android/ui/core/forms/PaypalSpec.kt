package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.LayoutSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val PaypalParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "paypal",
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val PaypalForm = LayoutSpec.create()

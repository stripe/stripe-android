package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.billingParams


internal val cardParams: MutableMap<String, Any?> = mutableMapOf(
    "number" to null,
    "exp_month" to null,
    "exp_year" to null,
    "cvc" to null,
//    "attribution" to listOf("PaymentSheet.Form")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val CardParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "card",
    "billing_details" to billingParams,
    "card" to cardParams
)

internal val creditDetailsSection = SectionSpec(
    IdentifierSpec.Generic("credit_details_section"),
    CardDetailsSpec
)

internal val creditBillingSection = SectionSpec(
    IdentifierSpec.Generic("credit_billing_section"),
    CardBillingSpec,
    R.string.billing_details
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val CardForm = LayoutSpec.create(
    creditDetailsSection,
    creditBillingSection,
    SaveForFutureUseSpec(emptyList())
)

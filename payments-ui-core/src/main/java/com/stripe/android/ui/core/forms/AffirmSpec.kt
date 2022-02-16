package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.KlarnaHelper
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.billingParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AffirmParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "affirm"
)

internal val affirmHeader = StaticTextSpec(
    identifier = IdentifierSpec.Generic("affirm_header"),
    stringResId = R.string.affirm_buy_now_pay_later,
    fontSizeSp = 13,
    letterSpacingSp = -.15,
    color = R.color.divider_text_color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AffirmForm = LayoutSpec.create(
    affirmHeader
)

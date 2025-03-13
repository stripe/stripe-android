package com.stripe.android.paymentsheet.ui

import com.stripe.android.uicore.forms.FormFieldEntry

internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val city: FormFieldEntry? = null,
    val country: FormFieldEntry? = null, // two-character country code
    val line1: FormFieldEntry? = null,
    val line2: FormFieldEntry? = null,
    val postalCode: FormFieldEntry? = null,
    val state: FormFieldEntry? = null
)
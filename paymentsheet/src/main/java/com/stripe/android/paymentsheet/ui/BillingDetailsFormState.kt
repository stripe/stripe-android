package com.stripe.android.paymentsheet.ui

import com.stripe.android.uicore.forms.FormFieldEntry

internal data class BillingDetailsFormState(
    val name: FormFieldEntry?,
    val email: FormFieldEntry?,
    val phone: FormFieldEntry?,
    val line1: FormFieldEntry?,
    val line2: FormFieldEntry?,
    val city: FormFieldEntry?,
    val postalCode: FormFieldEntry?,
    val state: FormFieldEntry?,
    val country: FormFieldEntry?,
)

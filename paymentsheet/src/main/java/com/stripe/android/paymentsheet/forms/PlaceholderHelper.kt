package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object PlaceholderHelper {
    /**
     * Returns the list of specs by adding or removing billing details fields.
     */
    @Suppress("UnusedParameter")
    internal fun specsForConfiguration(
        specs: List<FormItemSpec>,
        placeholderOverrideList: List<IdentifierSpec>,
        requiresMandate: Boolean,
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration,
        termsDisplay: PaymentSheet.TermsDisplay,
    ): List<FormItemSpec> {
        return specs
    }
}

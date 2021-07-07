package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec

internal class SetupIntentHiddenFieldController(
    identifiersHiddenOnSetupIntent: List<IdentifierSpec> = emptyList()
) : OptionalIdentifierListController(identifiersHiddenOnSetupIntent) {
    override val label: Int = R.string.invalid

    override fun onValueChange(setupIntentInUse: Boolean) {
        _enableHiding.value = setupIntentInUse
    }

    override fun onValueChange(setupIntentInUse: String) {
        _enableHiding.value = setupIntentInUse.toBooleanStrictOrNull() ?: true
    }
}

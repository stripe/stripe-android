package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.map

internal class SaveForFutureUseController(
    identifiersRequiredForFutureUse: List<IdentifierSpec> = emptyList()
) : OptionalIdentifierListController(identifiersRequiredForFutureUse) {
    override val label: Int = R.string.save_for_future_use
    val saveForFutureUse = enableHiding.map { !it }

    override fun onValueChange(saveForFutureUse: Boolean) {
        _enableHiding.value = !saveForFutureUse
    }

    override fun onValueChange(saveForFutureUse: String) {
        _enableHiding.value = !(saveForFutureUse.toBooleanStrictOrNull() ?: true)
    }
}

package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class SaveForFutureUseController(
    identifiersRequiredForFutureUse: List<IdentifierSpec> = emptyList()
) : Controller, OptionalIdentifierListController {
    override val label: Int = R.string.save_for_future_use
    private val _saveForFutureUse = MutableStateFlow(true)
    val saveForFutureUse: Flow<Boolean> = _saveForFutureUse
    override val fieldValue: Flow<String> = saveForFutureUse.map { it.toString() }
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    override val optionalIdentifiers: Flow<Set<IdentifierSpec>> =
        saveForFutureUse.map { saveFutureUseInstance ->
            identifiersRequiredForFutureUse
                .takeUnless { saveFutureUseInstance }
                ?.toSet()
                ?: emptySet()
        }

    fun onValueChange(saveForFutureUse: Boolean) {
        _saveForFutureUse.value = saveForFutureUse
    }

    override fun onValueChange(saveForFutureUseValue: String) {
        _saveForFutureUse.value = saveForFutureUseValue.toBooleanStrictOrNull() ?: true
    }
}

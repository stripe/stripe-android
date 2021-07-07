package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class SetupIntentHiddenFieldController(
    identifiersRequiredForFutureUse: List<IdentifierSpec> = emptyList()
) : Controller, OptionalIdentifierListController {
    override val label: Int = R.string.invalid
    private val _setupIntentInUse = MutableStateFlow(false)
    val setupIntentInUse: Flow<Boolean> = _setupIntentInUse
    override val fieldValue: Flow<String> = setupIntentInUse.map { it.toString() }
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    override val optionalIdentifiers: Flow<Set<IdentifierSpec>> =
        setupIntentInUse.map { setupIntentInUse ->
            identifiersRequiredForFutureUse.takeIf { setupIntentInUse }?.toSet() ?: emptySet()
        }

    fun onValueChange(setupIntentInUse: Boolean) {
        _setupIntentInUse.value = setupIntentInUse
    }

    override fun onValueChange(setupIntentInUse: String) {
        _setupIntentInUse.value = setupIntentInUse.toBooleanStrictOrNull() ?: true
    }
}

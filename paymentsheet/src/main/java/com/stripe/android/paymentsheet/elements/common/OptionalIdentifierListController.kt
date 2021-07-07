package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal sealed class OptionalIdentifierListController(
    identifiersRequiredForFutureUse: List<IdentifierSpec> = emptyList()
) : Controller {
    protected val _enableHiding = MutableStateFlow(false)
    val enableHiding: Flow<Boolean> = _enableHiding
    override val fieldValue: Flow<String> = enableHiding.map { it.toString() }
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    val optionalIdentifiers: Flow<Set<IdentifierSpec>> =
        enableHiding.map { enableHiding ->
            identifiersRequiredForFutureUse
                .takeIf { enableHiding }
                ?.toSet()
                ?: emptySet()
        }

    abstract fun onValueChange(disableHiding: Boolean)

    abstract override fun onValueChange(disableHiding: String)
}

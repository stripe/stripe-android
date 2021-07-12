package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.ElementType
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class SectionController(
    override val label: Int,
    val fieldIdentifiers: List<IdentifierSpec>
) : Controller {

    override val fieldValue: Flow<String> = MutableStateFlow("")

    override val rawFieldValue: Flow<String?> = MutableStateFlow(null)

    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    // This should be based off the fields in the section?
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val elementType: ElementType = ElementType.Section
    override fun onRawValueChange(rawValue: String) {}
}

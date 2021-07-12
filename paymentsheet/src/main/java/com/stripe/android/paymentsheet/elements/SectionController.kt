package com.stripe.android.paymentsheet.elements

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class SectionController(
    val label: Int?,
    val sectionFieldController: List<InputController>
) {
    val errorMessage: Flow<FieldError?> = combine(
        sectionFieldController.map {
            it.error
        }
    ) {
        it.firstOrNull()
    }
}

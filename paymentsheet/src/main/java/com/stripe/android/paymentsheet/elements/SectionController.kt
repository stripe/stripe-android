package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class SectionController(
    @StringRes val label: Int?,
    val sectionFieldController: List<InputController>
) : Controller {
    val error: Flow<FieldError?> = combine(
        sectionFieldController.map {
            it.error
        }
    ) {
        it.firstOrNull()
    }
}

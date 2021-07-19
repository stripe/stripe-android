package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class SectionController(
    @StringRes val label: Int?,
    val sectionFieldControllers: List<SectionFieldController>
) : Controller {
    val error: Flow<FieldError?> = combine(
        sectionFieldControllers.map {
            it.error
        }
    ) {
        it.firstOrNull()
    }
}

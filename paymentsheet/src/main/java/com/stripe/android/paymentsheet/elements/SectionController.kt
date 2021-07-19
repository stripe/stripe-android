package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * This is the controller for a section with a static number of fields.
 * This is in contrast to the [AddressController] which is a section in which the fields
 * in it change.
 */
internal class SectionController(
    @StringRes val label: Int?,
    val sectionFieldErrorControllers: List<SectionFieldErrorController>
) : Controller {
    val error: Flow<FieldError?> = combine(
        sectionFieldErrorControllers.map {
            it.error
        }
    ) {
        it.firstOrNull()
    }
}

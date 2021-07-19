package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.SectionFieldElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

internal class AddressController(
    val fieldsFlowable: Flow<List<SectionFieldElement>>
) : Controller, SectionFieldErrorController {
    @StringRes
    val label: Int? = null

    @ExperimentalCoroutinesApi
    override val error = fieldsFlowable.flatMapLatest { sectionFieldElements ->
        combine(sectionFieldElements.map { it.controller.error }) { fieldErrors ->
            fieldErrors.filterNotNull().firstOrNull()
        }
    }
}

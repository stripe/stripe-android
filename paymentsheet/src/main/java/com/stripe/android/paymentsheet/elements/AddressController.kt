package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.ElementType
import com.stripe.android.paymentsheet.SectionFieldElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

internal class AddressController(
    val fieldsFlowable: Flow<List<SectionFieldElement>>
) : Controller, SectionFieldController {
    @StringRes val label: Int? = null
    override val error = fieldsFlowable.flatMapLatest {
        combine(it.map { it.controller.error }) {
            it.filterNotNull().firstOrNull()
        }
    }
    override val elementType: ElementType = ElementType.Address
}

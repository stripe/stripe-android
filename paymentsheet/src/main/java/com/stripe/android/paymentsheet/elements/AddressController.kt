package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.SectionFieldElementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

internal class AddressController(
    @StringRes val label: Int?,
    val fieldsFlowable: Flow<List<SectionFieldElementType>>
) : Controller {
    val error = fieldsFlowable.flatMapLatest {
        combine(it.map { it.controller.error }) {
            it.filterNotNull().firstOrNull()
        }
    }
}

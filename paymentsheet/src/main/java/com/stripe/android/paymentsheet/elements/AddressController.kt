package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

internal class AddressController(
    val fieldsFlowable: Flow<List<SectionFieldElement>>
) : InputController {
    @StringRes
    override val label: Int = R.string.billing_details

    @ExperimentalCoroutinesApi
    override val error = fieldsFlowable.flatMapLatest { sectionFieldElements ->
        combine(sectionFieldElements.map { it.controller.error }) { fieldErrors ->
            fieldErrors.filterNotNull().firstOrNull()
        }
    }

    override val fieldValue: Flow<String>
        get() = TODO("Not yet implemented")
    override val rawFieldValue: Flow<String?>
        get() = TODO("Not yet implemented")
    override val isComplete: Flow<Boolean>
        get() = TODO("Not yet implemented")

    override fun onRawValueChange(rawValue: String) {
        TODO("Not yet implemented")
    }
}

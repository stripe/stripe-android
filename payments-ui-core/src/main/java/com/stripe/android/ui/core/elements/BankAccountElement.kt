package com.stripe.android.ui.core.elements

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

data class BankAccountElement(
    val state: State,
) : FormElement {

    override val identifier: IdentifierSpec = IdentifierSpec.Generic("bank_account")
    override val controller: InputController? = null

    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return stateFlowOf(emptyList())
    }

    data class State(
        val bankName: String?,
        val last4: String?,
        val showCheckbox: Boolean,
        val saveForFutureUseElement: SaveForFutureUseElement,
        val onRemoveAccount: () -> Unit,
    )
}

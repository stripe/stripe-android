package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BankAccountElement(
    val state: State,
    val isInstantDebits: Boolean,
) : FormElement {

    override val identifier: IdentifierSpec = IdentifierSpec("bank_account")
    override val controller: InputController? = null

    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        val formFieldValues = buildList {
            if (isInstantDebits) {
                add(IdentifierSpec.LinkPaymentMethodId to FormFieldEntry(state.id, isComplete = true))
            } else {
                add(IdentifierSpec.BankAccountId to FormFieldEntry(state.id, isComplete = true))
            }

            add(IdentifierSpec.BankName to FormFieldEntry(state.bankName, isComplete = true))
            add(IdentifierSpec.Last4 to FormFieldEntry(state.last4, isComplete = true))
            add(IdentifierSpec.UsesMicrodeposits to FormFieldEntry(state.usesMicrodeposits.toString(), isComplete = true))
        }

        return stateFlowOf(formFieldValues)
    }

    data class State(
        val id: String,
        val bankName: String?,
        val last4: String?,
        val usesMicrodeposits: Boolean,
        val onRemoveAccount: () -> Unit,
    )
}

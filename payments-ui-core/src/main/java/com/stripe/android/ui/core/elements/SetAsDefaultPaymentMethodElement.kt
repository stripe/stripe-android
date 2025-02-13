package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Element that allows users to set a new payment method as their default.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SetAsDefaultPaymentMethodElement(
    val initialValue: Boolean,
    val shouldShowElementFlow: StateFlow<Boolean>
) : FormElement {

    override val identifier: IdentifierSpec = IdentifierSpec.SetAsDefaultPaymentMethod

    override val controller: SetAsDefaultPaymentMethodController = SetAsDefaultPaymentMethodController(
        setAsDefaultPaymentMethodInitialValue = initialValue
    )
    override val allowsUserInteraction: Boolean = true

    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.formFieldValue.mapAsStateFlow {
            listOf(
                identifier to it
            )
        }
}

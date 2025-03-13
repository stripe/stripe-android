package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HAS_OTHER_PAYMENT_METHODS_DEFAULT_VALUE = true

/**
 * Element that allows users to set a new payment method as their default.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SetAsDefaultPaymentMethodElement(
    val initialValue: Boolean,
    val saveForFutureUseCheckedFlow: StateFlow<Boolean>,
    val hasOtherPaymentMethods: Boolean,
) : FormElement {

    val shouldShowElementFlow: StateFlow<Boolean> = saveForFutureUseCheckedFlow.mapAsStateFlow {
        it && hasOtherPaymentMethods
    }

    override val identifier: IdentifierSpec = IdentifierSpec.SetAsDefaultPaymentMethod

    override val controller: SetAsDefaultPaymentMethodController = SetAsDefaultPaymentMethodController(
        setAsDefaultPaymentMethodInitialValue = initialValue,
        saveForFutureUseCheckedFlow = saveForFutureUseCheckedFlow,
        hasOtherPaymentMethods = hasOtherPaymentMethods,
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

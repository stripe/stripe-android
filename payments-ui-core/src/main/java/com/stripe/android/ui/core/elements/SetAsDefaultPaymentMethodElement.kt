package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val IF_SAVED_SHOULD_SET_AS_DEFAULT_PAYMENT_METHOD_DEFAULT_VALUE = false

/**
 * Element that allows users to set a new payment method as their default.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SetAsDefaultPaymentMethodElement(
    val initialValue: Boolean,
    val saveForFutureUseCheckedFlow: StateFlow<Boolean>,
    val ifSavedShouldSetAsDefaultPaymentMethod: Boolean,
) : FormElement {

    val shouldShowElementFlow: StateFlow<Boolean> = saveForFutureUseCheckedFlow.mapAsStateFlow {
        it && !ifSavedShouldSetAsDefaultPaymentMethod
    }

    override val identifier: IdentifierSpec = IdentifierSpec.SetAsDefaultPaymentMethod

    override val controller: SetAsDefaultPaymentMethodController = SetAsDefaultPaymentMethodController(
        setAsDefaultPaymentMethodInitialValue = initialValue,
        saveForFutureUseCheckedFlow = saveForFutureUseCheckedFlow,
        ifSavedShouldSetAsDefaultPaymentMethod = ifSavedShouldSetAsDefaultPaymentMethod,
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

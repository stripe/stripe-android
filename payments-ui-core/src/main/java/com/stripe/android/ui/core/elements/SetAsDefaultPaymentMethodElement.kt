package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE = false

/**
 * Element that allows users to set a new payment method as their default.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SetAsDefaultPaymentMethodElement(
    val initialValue: Boolean,
    val saveForFutureUseCheckedFlow: StateFlow<Boolean>,
    val setAsDefaultMatchesSaveForFutureUse: Boolean,
) : FormElement {

    val shouldShowElementFlow: StateFlow<Boolean> = saveForFutureUseCheckedFlow.mapAsStateFlow {
        it && !setAsDefaultMatchesSaveForFutureUse
    }

    override val identifier: IdentifierSpec = IdentifierSpec.SetAsDefaultPaymentMethod

    override val controller: SetAsDefaultPaymentMethodController = SetAsDefaultPaymentMethodController(
        setAsDefaultPaymentMethodInitialValue = initialValue,
        saveForFutureUseCheckedFlow = saveForFutureUseCheckedFlow,
        setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
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

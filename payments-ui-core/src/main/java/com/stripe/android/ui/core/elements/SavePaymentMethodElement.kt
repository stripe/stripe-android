//package com.stripe.android.ui.core.elements
//
//import androidx.annotation.RestrictTo
//import com.stripe.android.core.strings.ResolvableString
//import com.stripe.android.uicore.elements.FormElement
//import com.stripe.android.uicore.elements.IdentifierSpec
//import com.stripe.android.uicore.elements.InputController
//import com.stripe.android.uicore.forms.FormFieldEntry
//import com.stripe.android.uicore.utils.mapAsStateFlow
//import kotlinx.coroutines.flow.StateFlow
//
//@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
//data class SavePaymentMethodElement(
//    val initialValue: Boolean,
//    val merchantName: String?,
//) : FormElement {
//    override val allowsUserInteraction: Boolean = true
//    override val mandateText: ResolvableString? = null
//    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
//        controller.formFieldValue.mapAsStateFlow {
//            listOf(
//                identifier to it
//            )
//        }
//
//    override val identifier: IdentifierSpec = IdentifierSpec.Generic("SavePaymentElement")
//    override val controller: InputController = object : InputController {
//
//    }
//
//}

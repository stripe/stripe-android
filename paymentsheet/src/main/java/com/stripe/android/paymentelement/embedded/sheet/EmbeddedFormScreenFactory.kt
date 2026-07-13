package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import javax.inject.Inject

internal fun interface EmbeddedFormScreenFactory {
    fun createFormScreen(code: PaymentMethodCode): EmbeddedNavigator.Screen.Form
}

internal class DefaultEmbeddedFormScreenFactory @Inject constructor(
    private val formFactory: EmbeddedNavigator.Screen.Form.Factory,
) : EmbeddedFormScreenFactory {
    override fun createFormScreen(code: PaymentMethodCode): EmbeddedNavigator.Screen.Form {
        return formFactory.create(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = code))
    }
}

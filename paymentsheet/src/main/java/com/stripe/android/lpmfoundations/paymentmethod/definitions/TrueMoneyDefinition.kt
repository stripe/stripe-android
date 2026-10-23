package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.FormElementDefinition
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormFieldId

internal object TrueMoneyDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.TrueMoney

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = emptySet()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = TrueMoneyUiDefinitionFactory
}

private object TrueMoneyUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = TrueMoneyDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_truemoney,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_truemoney,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        builder: FormElementsBuilder,
    ) {
        builder.addMandateIfAllowed(
            formElement = FormElementDefinition.mandate(
                identifier = FormFieldId.Generic("mandate"),
                stringResId = R.string.stripe_truemoney_mandate,
                args = listOf(metadata.merchantName),
                topPadding = 8.dp,
            ),
        )
    }
}

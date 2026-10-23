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

internal object NgUssdDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.NgUssd

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(AddPaymentMethodRequirement.UnsupportedForSetup)

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = NgUssdUiDefinitionFactory
}

private object NgUssdUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = NgUssdDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_ng_ussd,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_ng_ussd,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_ng_ussd_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        builder: FormElementsBuilder,
    ) {
        builder.footer(
            FormElementDefinition.mandate(
                identifier = FormFieldId.Generic("mandate"),
                stringResId = R.string.stripe_ng_payment_mor_notice,
                args = listOf(NIGERIAN_PAYMENT_METHOD_TERMS_URL),
                topPadding = 8.dp,
            )
        )
    }
}

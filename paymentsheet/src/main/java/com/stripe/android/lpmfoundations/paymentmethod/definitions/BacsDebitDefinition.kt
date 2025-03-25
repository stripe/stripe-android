package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal object BacsDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.BacsDebit

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = BacsDebitUiDefinitionFactory
}

private object BacsDebitUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = BacsDebitDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_bacs_debit,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        iconRequiresTinting = true,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements
    ): List<FormElement> {
        val localFields = listOfNotNull(
            PlaceholderSpec(
                apiPath = IdentifierSpec.Name,
                field = PlaceholderSpec.PlaceholderField.Name
            ),
            PlaceholderSpec(
                apiPath = IdentifierSpec.Email,
                field = PlaceholderSpec.PlaceholderField.Email
            ),
            PlaceholderSpec(
                apiPath = IdentifierSpec.Phone,
                field = PlaceholderSpec.PlaceholderField.Phone
            ),
            BacsDebitBankAccountSpec(),
            PlaceholderSpec(
                apiPath = IdentifierSpec.BillingAddress,
                field = PlaceholderSpec.PlaceholderField.BillingAddress
            ),
            BacsDebitConfirmSpec()
        )

        return transformSpecToElements.transform(
            metadata = metadata,
            specs = sharedDataSpec.fields + localFields,
            placeholderOverrideList = listOf(
                IdentifierSpec.Name,
                IdentifierSpec.Email,
                IdentifierSpec.BillingAddress
            )
        )
    }
}

package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.stripebalance.StripeBalanceFinancialAccountsElement
import com.stripe.android.ui.core.R

internal object StripeBalanceDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.StripeBalance

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.StripeBalanceFinancialAccounts,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = StripeBalanceUiDefinitionFactory
}

private object StripeBalanceUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = StripeBalanceDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_stripe_balance,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_stripe_balance,
        iconResourceNight = null,
        iconRequiresTinting = true,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        // Stripe balance is only offered once the buyer's financial accounts load
        val financialAccounts = metadata.stripeBalanceFinancialAccounts ?: return
        // The account picker comes first, before contact information and the billing address
        builder.header(
            StripeBalanceFinancialAccountsElement(
                financialAccounts = financialAccounts,
                initialFinancialAccountId = arguments.initialValues[
                    StripeBalanceFinancialAccountsElement.FINANCIAL_ACCOUNT_FIELD_ID
                ],
            )
        )
    }
}

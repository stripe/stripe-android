package com.stripe.android.lpmfoundations.paymentmethod.link

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.InstantDebitsDefinition
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankAccountElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal object LinkBankPaymentUiDefinitionFactory : UiDefinitionFactory.Simple {

    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = InstantDebitsDefinition.type.code,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_instant_debits,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            iconRequiresTinting = true,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        // TODO Current values!
        val defaultEmail = arguments.initialValues[IdentifierSpec.Email]
        val intermediateResult = arguments.intermediateResult

        val headerElement = StaticTextElement(
            identifier = IdentifierSpec.Generic("static_text"),
            stringResId = if (metadata.hasIntentToSetup()) {
                com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_save_bank_title
            } else {
                com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_pay_with_bank_title
            },
        )

        val emailElement = SectionElement.wrap(
            EmailElement(initialValue = defaultEmail)
        )

        val builder = FormElementsBuilder(arguments)
            .header(headerElement)
            .element(emailElement)

        if (intermediateResult is CollectBankAccountForInstantDebitsResult.Completed) {
            builder.element(
                BankAccountElement(
                    state = intermediateResult.toBankAccountElementState(
                        onRemoveAccount = { arguments.onRemoveBankAccount() },
                    ),
                    isInstantDebits = true,
                )
            )

            val showCheckbox = isSaveForFutureUseValueChangeable(
                code = PaymentMethod.Type.Link.code,
                intent = metadata.stripeIntent,
                paymentMethodSaveConsentBehavior = metadata.paymentMethodSaveConsentBehavior,
                hasCustomerConfiguration = metadata.hasCustomerConfiguration,
            )

            if (showCheckbox) {
                builder.element(
                    SaveForFutureUseElement(
                        initialValue = arguments.saveForFutureUseInitialValue,
                        merchantName = metadata.merchantName,
                    )
                )
            }
        } else if (intermediateResult is CollectBankAccountForInstantDebitsResult.Failed) {
            // TODO: Render error
        }

        return builder.build()
    }
}

private fun CollectBankAccountForInstantDebitsResult.Completed.toBankAccountElementState(
    onRemoveAccount: () -> Unit,
): BankAccountElement.State {
    return BankAccountElement.State(
        id = paymentMethodId,
        bankName = bankName,
        last4 = last4,
        onRemoveAccount = onRemoveAccount,
        usesMicrodeposits = false,
    )
}
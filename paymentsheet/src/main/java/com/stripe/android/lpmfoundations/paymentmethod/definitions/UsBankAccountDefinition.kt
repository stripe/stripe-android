package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory.Arguments.BankAccountAction
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.BankAccountElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.ui.core.R as StripeUiCoreR

internal object UsBankAccountDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.USBankAccount

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.ValidUsBankVerificationMethod,
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = UsBankAccountUiDefinitionFactory
}

private object UsBankAccountUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(): SupportedPaymentMethod = SupportedPaymentMethod(
        code = UsBankAccountDefinition.type.code,
        displayNameResource = StripeUiCoreR.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_bank,
        iconRequiresTinting = true,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
    )

    override fun createFormHeaderInformation(customerHasSavedPaymentMethods: Boolean): FormHeaderInformation {
        return createSupportedPaymentMethod().asFormHeaderInformation().copy(
            displayName = StripeUiCoreR.string.stripe_paymentsheet_add_us_bank_account.resolvableString,
            shouldShowIcon = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val defaultName = arguments.initialValues[IdentifierSpec.Name]
        val defaultEmail = arguments.initialValues[IdentifierSpec.Email]
        val intermediateResult = arguments.intermediateResult

        val headerElement = StaticTextElement(
            identifier = IdentifierSpec.Generic("static_text"),
            stringResId = if (metadata.hasIntentToSetup()) {
                R.string.stripe_paymentsheet_save_bank_title
            } else {
                R.string.stripe_paymentsheet_pay_with_bank_title
            },
        )

        val nameElement = SectionElement.wrap(
            SimpleTextElement(
                identifier = IdentifierSpec.Name,
                controller = NameConfig.createController(defaultName),
            )
        )

        val emailElement = SectionElement.wrap(
            EmailElement(initialValue = defaultEmail)
        )

        val builder = FormElementsBuilder(arguments)
            .header(headerElement)
            .element(nameElement)
            .element(emailElement)

        val bankAccountElementState = arguments.retrieveBankAccountElementState()

        if (bankAccountElementState != null) {
            builder.element(
                BankAccountElement(
                    state = bankAccountElementState,
                    isInstantDebits = false,
                )
            )

            val showCheckbox = isSaveForFutureUseValueChangeable(
                code = PaymentMethod.Type.USBankAccount.code,
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
        } else if (intermediateResult is CollectBankAccountResultInternal.Failed) {
            // TODO: Render error
        }

        return builder.build()
    }
}

private fun UiDefinitionFactory.Arguments.retrieveBankAccountElementState(): BankAccountElement.State? {
    val onRemoveBankAccount = {
        onBankAccountAction(BankAccountAction.Remove)
    }

    return if (intermediateResult is CollectBankAccountResultInternal.Completed) {
        intermediateResult.toBankAccountElementState(onRemoveAccount = onRemoveBankAccount)
    } else {
        val paymentMethodId = initialValues[IdentifierSpec.BankAccountId]
        val bankName = initialValues[IdentifierSpec.USBankAccountBankName]
        val last4 = initialValues[IdentifierSpec.USBankAccountLast4]
        val usesMicrodeposits = initialValues[IdentifierSpec.USBankAccountUsesMicrodeposits]?.toBoolean() ?: false

        if (paymentMethodId != null) {
            BankAccountElement.State(
                id = paymentMethodId,
                bankName = bankName,
                last4 = last4,
                usesMicrodeposits = usesMicrodeposits,
                onRemoveAccount = onRemoveBankAccount,
            )
        } else {
            null
        }
    }
}

private fun CollectBankAccountResultInternal.Completed.toBankAccountElementState(
    onRemoveAccount: () -> Unit,
): BankAccountElement.State {
    val usBankAccountData = requireNotNull(response.usBankAccountData)

    val bankName = when (val paymentAccount = usBankAccountData.financialConnectionsSession.paymentAccount) {
        is BankAccount -> paymentAccount.bankName
        is FinancialConnectionsAccount -> paymentAccount.institutionName
        null -> null
    }

    val last4 = when (val paymentAccount = usBankAccountData.financialConnectionsSession.paymentAccount) {
        is BankAccount -> paymentAccount.last4
        is FinancialConnectionsAccount -> paymentAccount.last4
        null -> null
    }

    val usesMicrodeposits = when (usBankAccountData.financialConnectionsSession.paymentAccount) {
        is BankAccount -> true
        is FinancialConnectionsAccount -> false
        null -> false
    }

    return BankAccountElement.State(
        id = usBankAccountData.financialConnectionsSession.id,
        bankName = bankName,
        last4 = last4,
        onRemoveAccount = onRemoveAccount,
        usesMicrodeposits = usesMicrodeposits,
    )
}

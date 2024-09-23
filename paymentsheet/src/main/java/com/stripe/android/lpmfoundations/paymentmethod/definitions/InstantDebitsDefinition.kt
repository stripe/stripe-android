package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkBankPaymentUiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.BankAccountElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.ui.core.R as PaymentsUiCoreR

internal object InstantDebitsDefinition : PaymentMethodDefinition {

    override val type: PaymentMethod.Type = PaymentMethod.Type.Link

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.InstantDebits,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = LinkBankPaymentUiDefinitionFactory
}

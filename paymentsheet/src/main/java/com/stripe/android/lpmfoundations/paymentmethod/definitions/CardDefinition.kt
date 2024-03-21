package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal object CardDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Card

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = CardUiDefinitionFactory
}

private object CardUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(sharedDataSpec: SharedDataSpec) = SupportedPaymentMethod(
        paymentMethodDefinition = CardDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
        tintIconOnSelection = true,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements
    ): List<FormElement> {
        val billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration
        val specs = listOf(
            ContactInformationSpec(
                collectName = false,
                collectEmail = billingDetailsCollectionConfiguration.collectsEmail,
                collectPhone = billingDetailsCollectionConfiguration.collectsPhone,
            ),
            CardDetailsSectionSpec(
                collectName = billingDetailsCollectionConfiguration.collectsName,
            ),
            CardBillingSpec(
                collectionMode = billingDetailsCollectionConfiguration.address.toInternal(),
            ),
            SaveForFutureUseSpec(),
        )
        return transformSpecToElements.transform(
            specs = specs,
            replacePlaceholders = false,
        )
    }
}

internal fun PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
.toInternal(): BillingDetailsCollectionConfiguration.AddressCollectionMode {
    return when (this) {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        }

        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        }

        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        }
    }
}

package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object CardDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Card

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return hardcodedCardSpec(metadata.billingDetailsCollectionConfiguration)
    }

    fun hardcodedCardSpec(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
    ): SupportedPaymentMethod {
        val specs = listOfNotNull(
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
            ).takeIf {
                billingDetailsCollectionConfiguration.collectsAddress
            },
            SaveForFutureUseSpec(),
        )
        return SupportedPaymentMethod(
            code = "card",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            tintIconOnSelection = true,
            formSpec = LayoutSpec(specs),
        )
    }
}


internal fun PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode?.toInternal(
): BillingDetailsCollectionConfiguration.AddressCollectionMode {
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

        else -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    }
}

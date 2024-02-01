package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.CardRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
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

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return hardcodedCardSpec(metadata.billingDetailsCollectionConfiguration)
    }

    fun hardcodedCardSpec(
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
    ): SupportedPaymentMethod {
        val specs = listOfNotNull(
            ContactInformationSpec(
                collectName = false,
                collectEmail = billingDetailsCollectionConfiguration.collectEmail,
                collectPhone = billingDetailsCollectionConfiguration.collectPhone,
            ),
            CardDetailsSectionSpec(
                collectName = billingDetailsCollectionConfiguration.collectName,
            ),
            CardBillingSpec(
                collectionMode = billingDetailsCollectionConfiguration.address,
            ).takeIf {
                billingDetailsCollectionConfiguration.collectAddress
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
            requirement = CardRequirement,
            formSpec = LayoutSpec(specs),
        )
    }
}

package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec

object LpmRepositoryTestHelpers {
    val card: SupportedPaymentMethod = CardDefinition.hardcodedCardSpec(BillingDetailsCollectionConfiguration())

    val usBankAccount: SupportedPaymentMethod = SupportedPaymentMethod(
        code = "us_bank_account",
        requiresMandate = true,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        tintIconOnSelection = true,
        requirement = USBankAccountRequirement,
        formSpec = LayoutSpec(emptyList())
    )
}

fun LpmRepository.updateFromDisk(stripeIntent: StripeIntent) {
    val metadata = PaymentMethodMetadataFactory.create(stripeIntent = stripeIntent)
    update(metadata = metadata, serverLpmSpecs = null)
}

fun LpmRepository.update(
    stripeIntent: StripeIntent,
    serverLpmSpecs: String? = null,
    billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
        BillingDetailsCollectionConfiguration(),
    financialConnectionsAvailable: Boolean = true
) {
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = stripeIntent,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        financialConnectionsAvailable = financialConnectionsAvailable,
    )
    update(metadata = metadata, serverLpmSpecs = serverLpmSpecs)
}

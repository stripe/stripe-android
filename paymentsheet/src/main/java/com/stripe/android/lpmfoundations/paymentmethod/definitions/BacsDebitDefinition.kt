package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.BacsDebitRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object BacsDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.BacsDebit

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
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

        return SupportedPaymentMethod(
            code = "bacs_debit",
            requiresMandate = true,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_bacs_debit,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = true,
            requirement = BacsDebitRequirement,
            formSpec = LayoutSpec(items = sharedDataSpec.fields + localFields),
            placeholderOverrideList = listOf(
                IdentifierSpec.Name,
                IdentifierSpec.Email,
                IdentifierSpec.BillingAddress
            )
        )
    }
}

package com.stripe.android.paymentsheet.prototype.paymentmethods

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.ContactInformationCollectionMode
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.KlarnaHelper
import com.stripe.android.uicore.stripeColors

internal object KlarnaPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Klarna

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_klarna)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            }

            header(KlarnaHeaderUiElementDefinition())
            requireContactInformation(ContactInformationCollectionMode.Email)

            if (requireBillingAddressCollection) {
                // TODO:
//                element(KlarnaCountryElementDefinition())
            } else {
                val availableCountries = KlarnaHelper.getAllowedCountriesForCurrency(metadata.stripeIntent.currency)
                requireBillingAddress(availableCountries = availableCountries)
            }
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.createKlarna()
        )
    }
}

private class KlarnaHeaderUiElementDefinition : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return KlarnaHeaderUiRenderer()
    }
}

private class KlarnaHeaderUiRenderer : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        Text(
            text = stringResource(id = KlarnaHelper.getKlarnaHeader()),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.stripeColors.subtitle,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {} // makes it a separate accessible item
        )
    }
}

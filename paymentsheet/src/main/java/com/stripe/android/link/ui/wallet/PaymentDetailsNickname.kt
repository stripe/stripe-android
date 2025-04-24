package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeUiCoreR

internal val ConsumerPaymentDetails.PaymentDetails.displayName: String
    @Composable
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> {
            nickname ?: makeFallbackCardName(funding, brand.displayName)
        }
        is ConsumerPaymentDetails.BankAccount -> {
            nickname ?: bankName ?: stringResource(StripeUiCoreR.string.stripe_payment_method_bank)
        }
        is ConsumerPaymentDetails.Passthrough -> {
            "•••• $last4"
        }
    }

@Composable
private fun makeFallbackCardName(funding: String, brand: String): String {
    return when (funding) {
        "CREDIT" -> stringResource(R.string.stripe_link_card_type_credit, brand)
        "DEBIT" -> stringResource(R.string.stripe_link_card_type_debit, brand)
        "PREPAID" -> stringResource(R.string.stripe_link_card_type_prepaid, brand)
        "CHARGE", "FUNDING_INVALID" -> stringResource(R.string.stripe_link_card_type_unknown, brand)
        else -> stringResource(R.string.stripe_link_card_type_unknown, brand)
    }
}

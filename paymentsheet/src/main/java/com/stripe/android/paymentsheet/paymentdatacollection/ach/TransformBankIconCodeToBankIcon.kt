package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.R as StripeR

internal fun transformBankIconCodeToBankIcon(
    iconCode: String?,
    fallbackIcon: Int,
): Int {
    return when (iconCode) {
        "boa" -> StripeR.drawable.stripe_ic_bank_boa
        "capitalone" -> StripeR.drawable.stripe_ic_bank_capitalone
        "citibank" -> StripeR.drawable.stripe_ic_bank_citi
        "compass" -> StripeR.drawable.stripe_ic_bank_compass
        "morganchase" -> StripeR.drawable.stripe_ic_bank_morganchase
        "nfcu" -> StripeR.drawable.stripe_ic_bank_nfcu
        "pnc" -> StripeR.drawable.stripe_ic_bank_pnc
        "suntrust" -> StripeR.drawable.stripe_ic_bank_suntrust
        "svb" -> StripeR.drawable.stripe_ic_bank_svb
        "stripe" -> StripeR.drawable.stripe_ic_bank_stripe
        "td" -> StripeR.drawable.stripe_ic_bank_td
        "usaa" -> StripeR.drawable.stripe_ic_bank_usaa
        "usbank" -> StripeR.drawable.stripe_ic_bank_usbank
        "wellsfargo" -> StripeR.drawable.stripe_ic_bank_wellsfargo
        else -> fallbackIcon
    }
}

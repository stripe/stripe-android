package com.stripe.android.paymentsheet.paymentdatacollection.ach

import kotlin.text.RegexOption.IGNORE_CASE
import com.stripe.android.R as StripeR

internal interface TransformToBankIcon {
    companion object {
        operator fun invoke(bankName: String?): Int {
            if (bankName == null) {
                return StripeR.drawable.stripe_ic_bank
            }

            val bankNameRegexIconMap = mapOf(
                Regex("Bank of America", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_boa,
                Regex("Capital One", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_capitalone,
                Regex("Citibank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_citi,
                Regex("BBVA|COMPASS", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_compass,
                Regex("MORGAN CHASE|JP MORGAN|Chase", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_morganchase,
                Regex("NAVY FEDERAL CREDIT UNION", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_nfcu,
                Regex("PNC\\s?BANK|PNC Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_pnc,
                Regex("SUNTRUST|SunTrust Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_suntrust,
                Regex("Silicon Valley Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_svb,
                Regex("Stripe|TestInstitution", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_stripe,
                Regex("TD Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_td,
                Regex("USAA FEDERAL SAVINGS BANK|USAA Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_usaa,
                Regex("U\\.?S\\. BANK|US Bank", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_usbank,
                Regex("Wells Fargo", IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_wellsfargo
            )

            return bankNameRegexIconMap
                .filter { it.key.findAll(bankName).any() }
                .firstNotNullOfOrNull { it.value }
                ?: StripeR.drawable.stripe_ic_bank
        }
    }
}

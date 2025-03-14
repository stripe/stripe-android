package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.R as StripeR

internal interface TransformToBankIcon {
    companion object {
        operator fun invoke(bankName: String?): Int {
            if (bankName == null) return StripeR.drawable.stripe_ic_bank_grey
            val bankNameRegexIconMap = mapOf(
                Regex("Bank of America", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_boa,
                Regex("Capital One", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_capitalone,
                Regex("Citibank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_citi,
                Regex("BBVA|COMPASS", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_compass,
                Regex("MORGAN CHASE|JP MORGAN|Chase", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_morganchase,
                Regex("NAVY FEDERAL CREDIT UNION", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_nfcu,
                Regex("PNC\\s?BANK|PNC Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_pnc,
                Regex("SUNTRUST|SunTrust Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_suntrust,
                Regex("Silicon Valley Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_svb,
                Regex("Stripe|TestInstitution", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_stripe,
                Regex("TD Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_td,
                Regex("USAA FEDERAL SAVINGS BANK|USAA Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_usaa,
                Regex("U\\.?S\\. BANK|US Bank", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_usbank,
                Regex("Wells Fargo", RegexOption.IGNORE_CASE) to
                    StripeR.drawable.stripe_ic_bank_wellsfargo
            )
            return bankNameRegexIconMap
                .filter { it.key.findAll(bankName).any() }
                .firstNotNullOfOrNull { it.value }
                ?: StripeR.drawable.stripe_ic_bank_grey
        }
    }
}

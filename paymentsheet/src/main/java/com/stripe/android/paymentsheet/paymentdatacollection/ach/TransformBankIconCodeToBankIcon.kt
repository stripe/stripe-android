package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.R as StripeR
import com.stripe.android.financialconnections.R as FinancialConnectionsR

internal object TransformBankIconCodeToBankIcon {
    operator fun invoke(
        iconCode: String?,
        fallbackIcon: Int = FinancialConnectionsR.drawable.stripe_ic_bank,
    ): Int {
        if (iconCode == null) {
            return fallbackIcon
        }
        val bankNameRegexIconMap = mapOf(
            Regex("boa", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_boa,
            Regex("capitalone", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_capitalone,
            Regex("citibank", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_citi,
            Regex("compass", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_compass,
            Regex("morganchase", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_morganchase,
            Regex("nfcu", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_nfcu,
            Regex("pnc", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_pnc,
            Regex("suntrust", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_suntrust,
            Regex("svb", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_svb,
            Regex("stripe", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_stripe,
            Regex("td", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_td,
            Regex("usaa", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_usaa,
            Regex("usbank", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_usbank,
            Regex("wellsfargo", RegexOption.IGNORE_CASE) to StripeR.drawable.stripe_ic_bank_wellsfargo,
        )
        return bankNameRegexIconMap
            .filter { it.key.findAll(iconCode).any() }
            .firstNotNullOfOrNull { it.value }
            ?: fallbackIcon
    }
}

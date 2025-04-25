package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.financialconnections.R as FinancialConnectionsR

internal object TransformToBankIcon {
    operator fun invoke(
        bankName: String?,
        fallbackIcon: Int = FinancialConnectionsR.drawable.stripe_ic_bank,
    ): Int {
        if (bankName == null) {
            return fallbackIcon
        }
        val bankNameRegexToIconCode = mapOf(
            Regex("Bank of America", RegexOption.IGNORE_CASE) to "boa",
            Regex("Capital One", RegexOption.IGNORE_CASE) to "capitalone",
            Regex("Citibank", RegexOption.IGNORE_CASE) to "citibank",
            Regex("BBVA|COMPASS", RegexOption.IGNORE_CASE) to "compass",
            Regex("MORGAN CHASE|JP MORGAN|Chase", RegexOption.IGNORE_CASE) to "morganchase",
            Regex("NAVY FEDERAL CREDIT UNION", RegexOption.IGNORE_CASE) to "nfcu",
            Regex("PNC\\s?BANK|PNC Bank", RegexOption.IGNORE_CASE) to "pnc",
            Regex("SUNTRUST|SunTrust Bank", RegexOption.IGNORE_CASE) to "suntrust",
            Regex("Silicon Valley Bank", RegexOption.IGNORE_CASE) to "svb",
            Regex("Stripe|TestInstitution", RegexOption.IGNORE_CASE) to "stripe",
            Regex("TD Bank", RegexOption.IGNORE_CASE) to "td",
            Regex("USAA FEDERAL SAVINGS BANK|USAA Bank", RegexOption.IGNORE_CASE) to "usaa",
            Regex("U\\.?S\\. BANK|US Bank", RegexOption.IGNORE_CASE) to "usbank",
            Regex("Wells Fargo", RegexOption.IGNORE_CASE) to "wellsfargo",
        )

        return bankNameRegexToIconCode
            .filter { it.key.findAll(bankName).any() }
            .firstNotNullOfOrNull {
                transformBankIconCodeToBankIcon(it.value, fallbackIcon)
            } ?: FinancialConnectionsR.drawable.stripe_ic_bank
    }
}

package com.stripe.android.paymentsheet.elements

internal enum class SupportedBankType(val assetFileName: String) {
    Eps("epsBanks.json"),
    Ideal("idealBanks.json"),
    P24("p24Banks.json")
}

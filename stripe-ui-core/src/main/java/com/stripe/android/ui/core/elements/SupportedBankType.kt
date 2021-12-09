package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class SupportedBankType(val assetFileName: String) {
    Eps("epsBanks.json"),
    Ideal("idealBanks.json"),
    P24("p24Banks.json")
}

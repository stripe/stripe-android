package com.stripe.android.connect.example.util

import com.stripe.android.connect.example.data.Merchant

object Fixtures {
    fun merchant(
        merchantId: String = "acct_123",
        displayName: String = "Example Inc."
    ): Merchant =
        Merchant(
            merchantId = merchantId,
            displayName = displayName
        )
}

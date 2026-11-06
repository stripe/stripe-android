package com.stripe.android.paymentsheet.paymentdatacollection.upi

import com.stripe.android.core.ApiConfiguration

internal object UpiFixtures {
    const val URL = "upi://pay?pa=merchant%40upi&pn=Merchant%20Name&am=100.00&cu=INR&tr=123"
    val ARGS = UpiAppChooserContract.Args(
        clientSecret = "pi_upi_secret_123",
        mobileAuthUrl = URL,
        apiConfiguration = ApiConfiguration.State("pk_test_upi", "acct_upi"),
    )
}

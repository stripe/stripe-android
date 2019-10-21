package com.stripe.android

internal object CustomerSessionTestHelper {
    internal fun setInstance(customerSession: CustomerSession) {
        CustomerSession.setInstance(customerSession)
    }
}

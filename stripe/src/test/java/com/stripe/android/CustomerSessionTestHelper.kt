package com.stripe.android

internal object CustomerSessionTestHelper {
    @JvmStatic
    fun setInstance(customerSession: CustomerSession) {
        CustomerSession.setInstance(customerSession)
    }
}

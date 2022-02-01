package com.stripe.android.stripe3ds2.service

import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import java.security.PublicKey
import java.security.cert.X509Certificate

/**
 * The main 3DS SDK interface. It shall provide methods to process transactions.
 */
interface StripeThreeDs2Service {
    fun createTransaction(
        sdkTransactionId: SdkTransactionId,
        directoryServerID: String,
        messageVersion: String?,
        isLiveMode: Boolean,
        directoryServerName: String,
        rootCerts: List<X509Certificate>,
        dsPublicKey: PublicKey,
        keyId: String?,
        uiCustomization: StripeUiCustomization
    ): Transaction

    /**
     * The cleanup method frees up resources that are used by the 3DS2 SDK. It is
     * called only once during a single app session.
     */
    fun cleanup()
}

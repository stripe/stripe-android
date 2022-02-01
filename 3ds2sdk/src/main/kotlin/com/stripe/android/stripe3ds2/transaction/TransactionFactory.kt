package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.security.EphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.views.Brand
import java.security.PublicKey
import java.security.cert.X509Certificate

/**
 * Factory to create a [Transaction].
 */
internal fun interface TransactionFactory {
    fun create(
        directoryServerId: String,
        rootCerts: List<X509Certificate>,
        directoryServerPublicKey: PublicKey,
        keyId: String?,
        sdkTransactionId: SdkTransactionId,
        isLiveMode: Boolean,
        brand: Brand
    ): Transaction
}

internal class DefaultTransactionFactory internal constructor(
    private val areqParamsFactory: AuthenticationRequestParametersFactory,
    private val ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
    private val sdkReferenceNumber: String
) : TransactionFactory {

    override fun create(
        directoryServerId: String,
        rootCerts: List<X509Certificate>,
        directoryServerPublicKey: PublicKey,
        keyId: String?,
        sdkTransactionId: SdkTransactionId,
        isLiveMode: Boolean,
        brand: Brand
    ): Transaction {
        val sdkKeyPair = ephemeralKeyPairGenerator.generate()
        return StripeTransaction(
            areqParamsFactory,
            directoryServerId,
            directoryServerPublicKey,
            keyId,
            sdkTransactionId,
            sdkKeyPair,
            sdkReferenceNumber
        )
    }
}

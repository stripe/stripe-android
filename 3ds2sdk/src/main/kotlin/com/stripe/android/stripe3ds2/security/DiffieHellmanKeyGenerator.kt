package com.stripe.android.stripe3ds2.security

import java.io.Serializable
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import javax.crypto.SecretKey

interface DiffieHellmanKeyGenerator : Serializable {
    fun generate(
        acsPublicKey: ECPublicKey,
        sdkPrivateKey: ECPrivateKey,
        agreementInfo: String
    ): SecretKey
}

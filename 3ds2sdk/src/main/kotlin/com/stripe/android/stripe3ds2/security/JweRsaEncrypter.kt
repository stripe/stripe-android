package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSAEncrypter
import java.security.interfaces.RSAPublicKey

/**
 * JWE Encrypter using RSA.
 */
internal class JweRsaEncrypter {
    @Throws(JOSEException::class)
    fun encrypt(payload: String, publicKey: RSAPublicKey, keyId: String?): String {
        val jwe = createJweObject(payload, keyId)
        jwe.encrypt(RSAEncrypter(publicKey))
        return jwe.serialize()
    }

    fun createJweObject(payload: String, keyId: String?): JWEObject {
        return JWEObject(
            JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128CBC_HS256)
                .keyID(keyId)
                .build(),
            Payload(payload)
        )
    }
}

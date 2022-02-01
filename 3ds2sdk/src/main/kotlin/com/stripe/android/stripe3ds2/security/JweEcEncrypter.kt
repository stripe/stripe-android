package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.text.ParseException

/**
 * JWE Encrypter using EC.
 */
internal class JweEcEncrypter private constructor(
    private val ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
    private val dhKeyGenerator: DiffieHellmanKeyGenerator
) {
    constructor(
        ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
        errorReporter: ErrorReporter
    ) : this(
        ephemeralKeyPairGenerator,
        StripeDiffieHellmanKeyGenerator(errorReporter)
    )

    @Throws(ParseException::class, JOSEException::class)
    fun encrypt(
        payload: String,
        acsPublicKey: ECPublicKey,
        directoryServerId: String
    ): String {
        JWTClaimsSet.parse(payload)
        val keyPair = ephemeralKeyPairGenerator.generate()
        val secretKey = dhKeyGenerator.generate(
            acsPublicKey,
            keyPair.private as ECPrivateKey, directoryServerId
        )
        val jwk = ECKey.Builder(Curve.P_256, keyPair.public as ECPublicKey)
            .build()
        val header = JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256)
            .ephemeralPublicKey(ECKey.parse(jwk.toJSONString()))
            .build()
        val jweObject = JWEObject(header, Payload(payload))
        jweObject.encrypt(DirectEncrypter(secretKey))
        return jweObject.serialize()
    }
}

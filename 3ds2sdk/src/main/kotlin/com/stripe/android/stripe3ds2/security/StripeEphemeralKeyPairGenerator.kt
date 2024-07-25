package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.jwk.Curve
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

/**
 * Generates an ephemeral keypair for Diffie-Hellman key exchange using the EC algorithm,
 * as required for AReq message.
 *
 * See "EMV® 3-D Secure SDK Technical Guide - 3.2 Diffie-Hellman process".
 *
 * The implementation first attempts to create the [KeyPair] using Android's default security
 * provider. This is based on guidance from Google
 * to not specify an explicit provider.
 *
 * https://android-developers.googleblog.com/2018/03/cryptography-changes-in-android-p.html
 */
class StripeEphemeralKeyPairGenerator(
    private val errorReporter: ErrorReporter
) : EphemeralKeyPairGenerator {

    override fun generate(): KeyPair {
        return runCatching {
            val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
            keyPairGenerator.initialize(ECGenParameterSpec(Curve.P_256.stdName))
            keyPairGenerator.generateKeyPair()
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    private companion object {
        private val ALGORITHM = Algorithm.EC.toString()
    }
}

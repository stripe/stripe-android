package com.stripe.android.stripe3ds2.security

import java.security.KeyPair

internal fun interface EphemeralKeyPairGenerator {
    fun generate(): KeyPair
}

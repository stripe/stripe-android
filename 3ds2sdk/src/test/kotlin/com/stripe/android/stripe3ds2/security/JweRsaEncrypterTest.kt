package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.RSADecrypter
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import kotlin.test.Test
import kotlin.test.assertEquals

class JweRsaEncrypterTest {

    private val jweRsaEncrypter = JweRsaEncrypter()

    @Test
    fun encryptDecryptRoundTrip_shouldReturnOriginalPayload() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.genKeyPair()

        val encryptedPayload = jweRsaEncrypter.encrypt(
            createPayload(),
            keyPair.public as RSAPublicKey,
            null
        )

        val jwe = JWEObject.parse(encryptedPayload)
        jwe.decrypt(RSADecrypter(keyPair.private))
        val payload = JSONObject(jwe.payload.toString())
        assertEquals("blue", payload.getString("color"))
        assertEquals("square", payload.getString("shape"))
    }

    @Test
    fun createJweObject_withKeyId_createsObjectWithkeyId() {
        val keyId = jweRsaEncrypter
            .createJweObject(createPayload(), "7c4debe3f4af7f9d1569a2ffea4343c2566826ee")
            .header
            .keyID
        assertEquals("7c4debe3f4af7f9d1569a2ffea4343c2566826ee", keyId)
    }

    private fun createPayload(): String {
        val originalPayload = JSONObject()
        originalPayload.put("color", "blue")
        originalPayload.put("shape", "square")
        return originalPayload.toString()
    }
}

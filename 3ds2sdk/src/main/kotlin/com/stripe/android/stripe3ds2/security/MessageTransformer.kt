package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.JOSEException
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseParseException
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.text.ParseException
import javax.crypto.SecretKey

interface MessageTransformer : Serializable {
    @Throws(JSONException::class, JOSEException::class)
    fun encrypt(challengeRequest: JSONObject, secretKey: SecretKey): String

    @Throws(
        ParseException::class, JOSEException::class, JSONException::class,
        ChallengeResponseParseException::class
    )
    fun decrypt(message: String, secretKey: SecretKey): JSONObject
}

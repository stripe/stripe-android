package com.stripe.attestation.domain

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.StripeRequest
import java.security.DigestException
import java.security.MessageDigest

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BuildRequestIdentifier {

    /**
     * Build a hashed request identifier from a [StripeRequest].
     */
    @Throws(DigestException::class)
    operator fun invoke(request: StripeRequest): String {
        val requestIdentifier = buildString {
            // TODO add more request details.
            append(request.url)
        }
        try {
            return sha256(requestIdentifier)
        } catch (exception: CloneNotSupportedException) {
            throw DigestException("couldn't make digest of partial content")
        }
    }

    /**
     * Build a hashed request identifier from a [String].
     */
    @Throws(DigestException::class)
    operator fun invoke(requestIdentifier: String): String {
        try {
            return sha256(requestIdentifier)
        } catch (exception: CloneNotSupportedException) {
            throw DigestException("couldn't make digest of partial content")
        }
    }

    private fun sha256(base: String): String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(base.toByteArray(charset("UTF-8")))
        val hexString = StringBuilder()
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        hexString.toString()
    } catch (ex: Exception) {
        throw DigestException(ex)
    }
}
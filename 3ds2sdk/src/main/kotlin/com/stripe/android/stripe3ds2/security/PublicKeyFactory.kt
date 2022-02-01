package com.stripe.android.stripe3ds2.security

import android.content.Context
import android.util.Base64
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.io.IOException
import java.io.InputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Scanner

internal class PublicKeyFactory(
    context: Context,
    private val errorReporter: ErrorReporter
) {
    private val context: Context = context.applicationContext

    /**
     * Returns the corresponding [PublicKey] for the given Directory Server ID.
     */
    fun create(directoryServerId: String): PublicKey {
        val directoryServer = DirectoryServer.lookup(directoryServerId)
        return if (directoryServer.isCertificate) {
            generateCertificate(directoryServer.fileName).publicKey
        } else {
            generatePublicKey(directoryServer.fileName, directoryServer.algorithm)
        }
    }

    private fun generateCertificate(fileName: String): Certificate {
        return runCatching {
            val factory = CertificateFactory.getInstance("X.509")
            factory.generateCertificate(readFile(fileName))
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    private fun generatePublicKey(fileName: String, algorithm: Algorithm): PublicKey {
        return runCatching {
            KeyFactory.getInstance(algorithm.toString())
                .generatePublic(X509EncodedKeySpec(readPublicKeyBytes(fileName)))
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    private fun readPublicKeyBytes(keyFile: String): ByteArray {
        return runCatching {
            val publicKey = Scanner(readFile(keyFile)).useDelimiter("\\A").next()
            Base64.decode(publicKey.toByteArray(), Base64.DEFAULT)
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    @Throws(IOException::class)
    private fun readFile(fileName: String): InputStream {
        return context.assets.open(fileName)
    }
}

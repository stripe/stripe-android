/*
 * Copyright 2020 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stripe.android.link.utils

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGenerator
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey

/**
 * Add a fake AndroidKeyStore provider, used by EncryptedSharedPreferences.
 *
 * Extended from https://proandroiddev.com/testing-jetpack-security-with-robolectric-9f9cf2aa4f61
 */
object FakeAndroidKeyStore {

    fun setup() {
        Security.addProvider(object : Provider("AndroidKeyStore", 1.0, "") {
            init {
                put("KeyStore.AndroidKeyStore", FakeKeyStore::class.java.name)
                put("KeyGenerator.AES", FakeAesKeyGenerator::class.java.name)
            }
        })
    }

    @Suppress("unused")
    class FakeKeyStore : KeyStoreSpi() {
        private val wrapped = KeyStore.getInstance(KeyStore.getDefaultType())
        private val secretKey = mock<SecretKey>().also {
            whenever(it.encoded).thenReturn("encoded".toByteArray())
        }

        override fun engineIsKeyEntry(alias: String?): Boolean = wrapped.isKeyEntry(alias)
        override fun engineIsCertificateEntry(alias: String?): Boolean =
            wrapped.isCertificateEntry(alias)

        override fun engineGetCertificate(alias: String?): Certificate =
            wrapped.getCertificate(alias)

        override fun engineGetCreationDate(alias: String?): Date = wrapped.getCreationDate(alias)
        override fun engineDeleteEntry(alias: String?) = wrapped.deleteEntry(alias)
        override fun engineSetKeyEntry(
            alias: String?,
            key: Key?,
            password: CharArray?,
            chain: Array<out Certificate>?
        ) =
            wrapped.setKeyEntry(alias, key, password, chain)

        override fun engineSetKeyEntry(
            alias: String?,
            key: ByteArray?,
            chain: Array<out Certificate>?
        ) = wrapped.setKeyEntry(alias, key, chain)

        override fun engineStore(stream: OutputStream?, password: CharArray?) =
            wrapped.store(stream, password)

        override fun engineSize(): Int = wrapped.size()
        override fun engineAliases(): Enumeration<String> = wrapped.aliases()
        override fun engineContainsAlias(alias: String?): Boolean = wrapped.containsAlias(alias)
        override fun engineLoad(stream: InputStream?, password: CharArray?) =
            wrapped.load(stream, password)

        override fun engineGetCertificateChain(alias: String?): Array<Certificate> =
            wrapped.getCertificateChain(alias)

        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) =
            wrapped.setCertificateEntry(alias, cert)

        override fun engineGetCertificateAlias(cert: Certificate?): String =
            wrapped.getCertificateAlias(cert)

        override fun engineGetKey(alias: String?, password: CharArray?): Key = secretKey
    }

    @Suppress("unused")
    class FakeAesKeyGenerator : KeyGeneratorSpi() {
        private val wrapped = KeyGenerator.getInstance("AES")

        override fun engineInit(random: SecureRandom?) = Unit
        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) = Unit
        override fun engineInit(keysize: Int, random: SecureRandom?) = Unit
        override fun engineGenerateKey(): SecretKey = wrapped.generateKey()
    }
}

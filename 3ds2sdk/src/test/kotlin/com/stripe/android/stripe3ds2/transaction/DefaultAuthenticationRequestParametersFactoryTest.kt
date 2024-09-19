package com.stripe.android.stripe3ds2.transaction

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nimbusds.jose.jwk.KeyUse
import com.stripe.android.stripe3ds2.CertificateFixtures
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.init.DeviceDataFactoryImpl
import com.stripe.android.stripe3ds2.init.DeviceParamNotAvailableFactoryImpl
import com.stripe.android.stripe3ds2.init.FakeAppInfoRepository
import com.stripe.android.stripe3ds2.init.SecurityChecker
import com.stripe.android.stripe3ds2.init.Warning
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.DefaultJweEncrypter
import com.stripe.android.stripe3ds2.security.DirectoryServer
import com.stripe.android.stripe3ds2.security.JweEncrypter
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.PublicKey
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthenticationRequestParametersFactoryTest {
    private val errorReporter = FakeErrorReporter()
    private val securityChecker = SecurityChecker {
        listOf(
            Warning(
                id = "SW01",
                message = "message",
                severity = Warning.Severity.HIGH
            )
        )
    }

    private val messageVersionRegistry = MessageVersionRegistry()
    private val ephemeralKeyPairGenerator = StripeEphemeralKeyPairGenerator(errorReporter)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun getDeviceDataJson_shouldCreateCorrectJsonString() = runTest {
        val rootJson = createFactory().deviceDataJson(SDK_TRANSACTION_ID)
        assertEquals(
            "1.6",
            rootJson.get(DefaultAuthenticationRequestParametersFactory.KEY_DATA_VERSION)
        )

        val deviceDataJson =
            rootJson.getJSONObject(DefaultAuthenticationRequestParametersFactory.KEY_DEVICE_DATA)
        assertEquals("Android", deviceDataJson.getString("C001"))
        assertEquals("unknown||robolectric", deviceDataJson.getString("C002"))

        val unavailableDeviceParamsJson = rootJson.getJSONObject(
            DefaultAuthenticationRequestParametersFactory.KEY_DEVICE_PARAM_NOT_AVAILABLE
        )
        assertEquals("RE01", unavailableDeviceParamsJson.getString("C010"))
        assertEquals("RE03", unavailableDeviceParamsJson.getString("C011"))

        val securityWarnings =
            rootJson.getJSONArray(DefaultAuthenticationRequestParametersFactory.KEY_SECURITY_WARNINGS)
        assertEquals("SW01", securityWarnings.getString(0))
    }

    @Test
    fun getSDKTransactionId_shouldReturnProvidedTransactionId() = runTest {
        assertThat(createParams().sdkTransactionId)
            .isEqualTo(SDK_TRANSACTION_ID)
    }

    @Test
    fun getDeviceData_withRsaKey_shouldReturnStringOfExpectedLength() = runTest {
        val encryptedDeviceData = createParams().deviceData
        assertNotNull(encryptedDeviceData)
        assertThat(encryptedDeviceData)
            .hasLength(3841)
    }

    @Test
    fun getDeviceData_withECKey_shouldReturnStringOfExpectedLength() = runTest {
        val encryptedDeviceData = createParams(
            EC_DIRECTORY_SERVER_ID,
            CertificateFixtures.DS_CERTIFICATE_RSA.publicKey
        ).deviceData
        assertThat(encryptedDeviceData.length)
            .isEqualTo(3841)
    }

    @Test
    fun getDeviceData_withRsaCert_shouldReturnStringOfExpectedLength() = runTest {
        val encryptedDeviceData = createParams(
            RSA_DIRECTORY_SERVER_ID,
            CertificateFixtures.DS_CERTIFICATE_RSA.publicKey
        ).deviceData
        assertThat(encryptedDeviceData.length)
            .isEqualTo(3841)
    }

    @Test
    fun getSDKEphemeralPublicKey_withEcPublicKey_shouldReturnCorrectJwk() = runTest {
        val jwkJson = createParams(
            EC_DIRECTORY_SERVER_ID,
            CertificateFixtures.DS_CERTIFICATE_RSA.publicKey
        ).sdkEphemeralPublicKey

        val jwk = JSONObject(jwkJson)
        assertTrue(jwk.has("kty"))
        assertTrue(jwk.has("use"))
        assertTrue(jwk.has("crv"))
        assertTrue(jwk.has("x"))
        assertTrue(jwk.has("y"))

        assertThat(jwk.length())
            .isEqualTo(5)

        assertEquals("EC", jwk.getString("kty"))
        assertEquals("sig", jwk.getString("use"))
        assertEquals("P-256", jwk.getString("crv"))
    }

    @Test
    fun `createPublicJwk with KeyUse should return expected object`() {
        val jwk = DefaultAuthenticationRequestParametersFactory.createPublicJwk(
            generatePublicKey(),
            keyId = "12345",
            keyUse = KeyUse.SIGNATURE
        )
        assertFalse(jwk.isPrivate)

        val jwkObject = JSONObject(jwk.toJSONString())
        assertTrue(jwkObject.has("kty"))
        assertTrue(jwkObject.has("use"))
        assertTrue(jwkObject.has("crv"))
        assertTrue(jwkObject.has("kid"))
        assertTrue(jwkObject.has("x"))
        assertTrue(jwkObject.has("y"))

        assertEquals(6, jwkObject.length())

        assertEquals("EC", jwkObject.getString("kty"))
        assertEquals("sig", jwkObject.getString("use"))
        assertEquals("P-256", jwkObject.getString("crv"))
    }

    @Test
    fun `createPublicJwk without KeyUse should not include use field`() {
        val jwk = DefaultAuthenticationRequestParametersFactory.createPublicJwk(
            generatePublicKey(),
            keyId = "12345",
            keyUse = null
        )

        val jwkObject = JSONObject(jwk.toJSONString())
        assertFalse(jwkObject.has("use"))
    }

    @Test
    fun `createPublicJwk with blank keyId should not include kid field`() {
        val jwk = DefaultAuthenticationRequestParametersFactory.createPublicJwk(
            generatePublicKey(),
            keyId = "      ",
            keyUse = null
        )

        val jwkObject = JSONObject(jwk.toJSONString())
        assertFalse(jwkObject.has("kid"))
    }

    @Test
    fun sdkAppId_shouldReturnAppId() = runTest {
        assertEquals(SDK_APP_ID, createParams().sdkAppId)
    }

    @Test
    fun sdkReferenceNumber_shouldReturnSDKReferenceNumber() = runTest {
        assertEquals(SDK_REFERENCE_NUMBER, createParams().sdkReferenceNumber)
    }

    @Test
    fun getMessageVersion_shouldReturnMessageVersion() = runTest {
        assertEquals(
            messageVersionRegistry.current,
            createParams().messageVersion
        )
    }

    @Test
    fun `getKeyUse() for directory server with null keyUse should return expected value`() {
        assertThat(
            createFactory().getKeyUse(DirectoryServer.Discover.ids.first())
        ).isNull()
    }

    @Test
    fun `getKeyUse() for unknown directory server should return expected value`() {
        assertThat(
            createFactory().getKeyUse("123456")
        ).isEqualTo(KeyUse.SIGNATURE)
    }

    @Test
    fun `create() when error should throw SDKRuntimeException`() = runTest {
        val jweEncrypter = JweEncrypter { _, _, _, _ -> error("Failed to encrypt.") }
        val failingFactory = createFactory(jweEncrypter)

        val error = assertFailsWith<SDKRuntimeException> {
            failingFactory.create(
                "123",
                CertificateFixtures.DS_CERTIFICATE_RSA.publicKey,
                "123",
                SdkTransactionId("123"),
                generatePublicKey()
            )
        }
        assertThat(error.message)
            .isEqualTo("Failed to encrypt.")
    }

    private suspend fun createParams(
        directoryServerId: String = RSA_DIRECTORY_SERVER_ID,
        publicKey: PublicKey = CertificateFixtures.DS_CERTIFICATE_RSA.publicKey
    ): AuthenticationRequestParameters {
        return createFactory()
            .create(
                directoryServerId,
                publicKey,
                null,
                SDK_TRANSACTION_ID,
                generatePublicKey()
            )
    }

    private fun createFactory(
        jweEncrypter: JweEncrypter = DefaultJweEncrypter(
            ephemeralKeyPairGenerator,
            errorReporter
        )
    ): DefaultAuthenticationRequestParametersFactory {
        return DefaultAuthenticationRequestParametersFactory(
            DeviceDataFactoryImpl(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                appInfoRepository = FakeAppInfoRepository(),
                messageVersionRegistry = messageVersionRegistry
            ),
            DeviceParamNotAvailableFactoryImpl(apiVersion = Build.VERSION.SDK_INT),
            securityChecker,
            FakeAppInfoRepository(SDK_APP_ID),
            jweEncrypter,
            messageVersionRegistry,
            SDK_REFERENCE_NUMBER,
            errorReporter,
            testDispatcher
        )
    }

    private fun generatePublicKey(): PublicKey {
        return ephemeralKeyPairGenerator
            .generate()
            .public
    }

    private companion object {
        private const val RSA_DIRECTORY_SERVER_ID = "F000000000"
        private const val EC_DIRECTORY_SERVER_ID = "F000000001"

        private const val SDK_REFERENCE_NUMBER = "3DS_LOA_SDK_12345"

        private val SDK_TRANSACTION_ID = ChallengeMessageFixtures.SDK_TRANS_ID

        private val SDK_APP_ID = UUID.randomUUID().toString()
    }
}

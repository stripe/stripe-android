package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [AnalyticsDataFactory].
 */
@RunWith(RobolectricTestRunner::class)
class AnalyticsDataFactoryTest {

    private val analyticsDataFactory =
        AnalyticsDataFactory.create(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun getTokenCreationParams_withValidInput_createsCorrectMap() {
        val tokens = listOf("CardInputView")
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedTokenName = AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION)

        val params = analyticsDataFactory.getTokenCreationParams(
            tokens,
            API_KEY,
            Token.TokenType.PII)
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals((AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1).toLong(), params.size.toLong())
        assertEquals(expectedTokenName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.TokenType.PII, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        val tokens = listOf("CardInputView")
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedTokenName = AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION)

        val params = analyticsDataFactory.getTokenCreationParams(
            tokens,
            API_KEY,
            Token.TokenType.CVC_UPDATE)
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals((AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1).toLong(), params.size.toLong())
        assertEquals(expectedTokenName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.TokenType.CVC_UPDATE, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getSourceCreationParams_withValidInput_createsCorrectMap() {
        // Size is SIZE-1 because tokens don't have a token_type field
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1
        val tokens = listOf("CardInputView")
        val loggingParams = analyticsDataFactory.getSourceCreationParams(
            tokens,
            API_KEY,
            Source.SourceType.SEPA_DEBIT)
        assertEquals(expectedSize.toLong(), loggingParams.size.toLong())
        assertEquals(Source.SourceType.SEPA_DEBIT,
            loggingParams[AnalyticsDataFactory.FIELD_SOURCE_TYPE])
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.SOURCE_CREATION),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(AnalyticsDataFactory.analyticsUa,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getPaymentMethodCreationParams() {
        val loggingParams = analyticsDataFactory
            .createPaymentMethodCreationParams(API_KEY, "pm_12345")
        assertNotNull(loggingParams)
        assertEquals(API_KEY,
            loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals("pm_12345",
            loggingParams[AnalyticsDataFactory.FIELD_PAYMENT_METHOD_ID])
        assertEquals(
            AnalyticsDataFactory.getEventParamName(
                AnalyticsDataFactory.EventName.CREATE_PAYMENT_METHOD),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(AnalyticsDataFactory.analyticsUa,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1
        val tokens = listOf("CardInputView")
        val loggingParams = analyticsDataFactory.getPaymentIntentConfirmationParams(
            tokens,
            API_KEY, null)
        assertEquals(expectedSize.toLong(), loggingParams.size.toLong())
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.CONFIRM_PAYMENT_INTENT),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(AnalyticsDataFactory.analyticsUa,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1
        val tokens = listOf("CardInputView")
        val loggingParams = analyticsDataFactory.getPaymentIntentRetrieveParams(
            tokens,
            API_KEY)
        assertEquals(expectedSize.toLong(), loggingParams.size.toLong())
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(AnalyticsDataFactory.getEventParamName(
            AnalyticsDataFactory.EventName.RETRIEVE_PAYMENT_INTENT),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(AnalyticsDataFactory.analyticsUa,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getSetupIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val params = analyticsDataFactory.getSetupIntentConfirmationParams(API_KEY, "card")
        assertEquals("card", params[AnalyticsDataFactory.FIELD_PAYMENT_METHOD_TYPE])
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        val tokens = listOf("CardInputView")
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedTokenName = AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION)
        val expectedUaName = AnalyticsDataFactory.analyticsUa

        val versionCode = 20
        val packageName = BuildConfig.APPLICATION_ID
        val packageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionCode = versionCode
        packageInfo.packageName = BuildConfig.APPLICATION_ID
        `when`(packageManager.getPackageInfo(packageName, 0))
            .thenReturn(packageInfo)

        val params = AnalyticsDataFactory(packageManager, packageName)
            .getTokenCreationParams(tokens, API_KEY, Token.TokenType.CARD)
        assertEquals((AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1).toLong(), params.size.toLong())
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(EXPECTED_SINGLE_TOKEN_LIST, params[AnalyticsDataFactory.FIELD_PRODUCT_USAGE])
        assertEquals(Token.TokenType.CARD, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])
        assertEquals(versionCode, params[AnalyticsDataFactory.FIELD_APP_VERSION])
        assertEquals(BuildConfig.APPLICATION_ID, params[AnalyticsDataFactory.FIELD_APP_NAME])

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedTokenName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[AnalyticsDataFactory.FIELD_ANALYTICS_UA])

        assertEquals("unknown_Android_robolectric",
            params[AnalyticsDataFactory.FIELD_DEVICE_TYPE])
    }

    @Test
    fun getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedTokenName = AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.SOURCE_CREATION)
        val expectedUaName = AnalyticsDataFactory.analyticsUa

        val params = analyticsDataFactory.getSourceCreationParams(null, API_KEY, Token.TokenType.BANK_ACCOUNT)
        assertEquals((AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2).toLong(), params.size.toLong())
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(Token.TokenType.BANK_ACCOUNT, params[AnalyticsDataFactory.FIELD_SOURCE_TYPE])

        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedTokenName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[AnalyticsDataFactory.FIELD_ANALYTICS_UA])

        assertEquals("unknown_Android_robolectric", params[AnalyticsDataFactory.FIELD_DEVICE_TYPE])
    }

    @Test
    fun addNameAndVersion_whenApplicationContextIsNull_addsNoContextValues() {
        val paramsMap = AnalyticsDataFactory(null, null)
            .createNameAndVersionParams()
        assertEquals(AnalyticsDataFactory.NO_CONTEXT, paramsMap[AnalyticsDataFactory.FIELD_APP_NAME])
        assertEquals(AnalyticsDataFactory.NO_CONTEXT, paramsMap[AnalyticsDataFactory.FIELD_APP_VERSION])
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun addNameAndVersion_whenPackageInfoNotFound_addsUnknownValues() {
        val packageName = "dummy_name"
        val manager = mock(PackageManager::class.java)

        `when`(manager.getPackageInfo(packageName, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val paramsMap = AnalyticsDataFactory(manager, packageName)
            .createNameAndVersionParams()
        assertEquals(AnalyticsDataFactory.UNKNOWN, paramsMap[AnalyticsDataFactory.FIELD_APP_NAME])
        assertEquals(AnalyticsDataFactory.UNKNOWN, paramsMap[AnalyticsDataFactory.FIELD_APP_VERSION])
    }

    @Test
    fun getEventParamName_withTokenCreation_createsExpectedParameter() {
        val expectedEventParam = "stripe_android.token_creation"
        assertEquals(expectedEventParam,
            AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION))
    }

    @Test
    fun getAnalyticsUa_returnsExpectedValue() {
        val androidAnalyticsUserAgent = "analytics.stripe_android-1.0"
        assertEquals(androidAnalyticsUserAgent, AnalyticsDataFactory.analyticsUa)
    }

    private companion object {
        private const val API_KEY = "pk_abc123"
        private val EXPECTED_SINGLE_TOKEN_LIST = listOf("CardInputView")
    }
}

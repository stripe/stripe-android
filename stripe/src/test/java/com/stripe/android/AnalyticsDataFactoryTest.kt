package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.PaymentMethod
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

    private val analyticsDataFactory = AnalyticsDataFactory(
        ApplicationProvider.getApplicationContext<Context>()
    )

    @Test
    fun getTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEvent = AnalyticsEvent.TokenCreate.toString()

        val params = analyticsDataFactory.createTokenCreationParams(
            ATTRIBUTION,
            API_KEY,
            Token.TokenType.PII
        )
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(
            AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1,
            params.size
        )
        assertEquals(expectedEvent, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.TokenType.PII, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.TokenCreate.toString()

        val params = analyticsDataFactory.createTokenCreationParams(
            ATTRIBUTION,
            API_KEY,
            Token.TokenType.CVC_UPDATE
        )
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(
            AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1,
            params.size
        )
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.TokenType.CVC_UPDATE, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getSourceCreationParams_withValidInput_createsCorrectMap() {
        // Size is SIZE-1 because tokens don't have a token_type field
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1
        val loggingParams = analyticsDataFactory.createSourceCreationParams(
            API_KEY,
            Source.SourceType.SEPA_DEBIT,
            ATTRIBUTION
        )
        assertEquals(expectedSize, loggingParams.size)
        assertEquals(Source.SourceType.SEPA_DEBIT,
            loggingParams[AnalyticsDataFactory.FIELD_SOURCE_TYPE])
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.SourceCreate.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(AnalyticsDataFactory.ANALYTICS_UA,
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
            AnalyticsEvent.PaymentMethodCreate.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun createPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2
        val loggingParams =
            analyticsDataFactory.createPaymentIntentConfirmationParams(
                API_KEY,
                PaymentMethod.Type.Card.code
            )
        assertEquals(expectedSize, loggingParams.size)
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.PaymentIntentConfirm.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        val expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2
        val loggingParams = analyticsDataFactory.createParams(
            AnalyticsEvent.PaymentIntentRetrieve,
            API_KEY
        )
        assertEquals(expectedSize, loggingParams.size)
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.PaymentIntentRetrieve.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA])
    }

    @Test
    fun getSetupIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val params = analyticsDataFactory.createSetupIntentConfirmationParams(
            API_KEY,
            PaymentMethod.Type.Card.code,
            "seti_12345"
        )
        assertEquals("card", params[AnalyticsDataFactory.FIELD_PAYMENT_METHOD_TYPE])
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.TokenCreate.toString()
        val expectedUaName = AnalyticsDataFactory.ANALYTICS_UA

        val versionCode = 20
        val packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        val packageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo().also {
            it.versionCode = versionCode
            it.packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        }
        `when`(packageManager.getPackageInfo(packageName, 0))
            .thenReturn(packageInfo)

        val params = AnalyticsDataFactory(packageManager, packageName)
            .createTokenCreationParams(
                ATTRIBUTION,
                API_KEY,
                Token.TokenType.CARD
            )
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1, params.size)
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(ATTRIBUTION.toList(), params[AnalyticsDataFactory.FIELD_PRODUCT_USAGE])
        assertEquals(Token.TokenType.CARD, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])
        assertEquals(versionCode, params[AnalyticsDataFactory.FIELD_APP_VERSION])
        assertEquals(BuildConfig.LIBRARY_PACKAGE_NAME, params[AnalyticsDataFactory.FIELD_APP_NAME])

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[AnalyticsDataFactory.FIELD_ANALYTICS_UA])

        assertEquals("unknown_Android_robolectric",
            params[AnalyticsDataFactory.FIELD_DEVICE_TYPE])
    }

    @Test
    fun getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.SourceCreate.toString()
        val expectedUaName = AnalyticsDataFactory.ANALYTICS_UA

        val params = analyticsDataFactory.createSourceCreationParams(
            API_KEY, Token.TokenType.BANK_ACCOUNT
        )
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2, params.size)
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(Token.TokenType.BANK_ACCOUNT, params[AnalyticsDataFactory.FIELD_SOURCE_TYPE])

        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
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
        assertEquals(
            expectedEventParam,
            AnalyticsEvent.TokenCreate.toString()
        )
    }

    @Test
    fun getAnalyticsUa_returnsExpectedValue() {
        val androidAnalyticsUserAgent = "analytics.stripe_android-1.0"
        assertEquals(androidAnalyticsUserAgent, AnalyticsDataFactory.ANALYTICS_UA)
    }

    private companion object {
        private const val API_KEY = "pk_abc123"
        private val ATTRIBUTION = setOf("CardInputView")
    }
}

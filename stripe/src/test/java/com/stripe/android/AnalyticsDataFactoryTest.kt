package com.stripe.android

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class for [AnalyticsDataFactory].
 */
@RunWith(RobolectricTestRunner::class)
class AnalyticsDataFactoryTest {
    private val packageManager = mock<PackageManager>()

    private val analyticsDataFactory = AnalyticsDataFactory(
        ApplicationProvider.getApplicationContext(),
        API_KEY
    )

    @Test
    fun getTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEvent = AnalyticsEvent.TokenCreate.toString()

        val params = analyticsDataFactory.createTokenCreationParams(
            ATTRIBUTION,
            Token.Type.Pii
        )
        // Size is SIZE-1 because tokens don't have a source_type field
        assertThat(params)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1)

        assertEquals(expectedEvent, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.Type.Pii.code, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.TokenCreate.toString()

        val params = analyticsDataFactory.createTokenCreationParams(
            ATTRIBUTION,
            Token.Type.CvcUpdate
        )
        // Size is SIZE-1 because tokens don't have a source_type field
        assertThat(params)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1)
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(Token.Type.CvcUpdate.code, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getSourceCreationParams_withValidInput_createsCorrectMap() {
        val loggingParams = analyticsDataFactory.createSourceCreationParams(
            Source.SourceType.SEPA_DEBIT,
            ATTRIBUTION
        )

        // Size is SIZE-1 because tokens don't have a token_type field
        assertThat(loggingParams)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1)

        assertEquals(
            Source.SourceType.SEPA_DEBIT,
            loggingParams[AnalyticsDataFactory.FIELD_SOURCE_TYPE]
        )
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.SourceCreate.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(
            AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA]
        )
    }

    @Test
    fun getPaymentMethodCreationParams() {
        val expectedParams = mapOf(
            "analytics_ua" to "analytics.stripe_android-1.0",
            "event" to "stripe_android.payment_method_creation",
            "publishable_key" to "pk_abc123",
            "os_name" to "REL",
            "os_release" to "9",
            "os_version" to 28,
            "device_type" to "unknown_Android_robolectric",
            "bindings_version" to BuildConfig.VERSION_NAME,
            "app_name" to "com.stripe.android.test",
            "app_version" to 0,
            "product_usage" to ATTRIBUTION.toList(),
            "source_type" to "card",
            "payment_method_id" to "pm_12345"
        )

        val actualParams = analyticsDataFactory
            .createPaymentMethodCreationParams(
                "pm_12345",
                PaymentMethod.Type.Card,
                ATTRIBUTION
            )

        assertThat(actualParams)
            .isEqualTo(expectedParams)
    }

    @Test
    fun createPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val loggingParams =
            analyticsDataFactory.createPaymentIntentConfirmationParams(
                PaymentMethod.Type.Card.code
            )

        assertThat(loggingParams)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2)

        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.PaymentIntentConfirm.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(
            AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA]
        )
    }

    @Test
    fun getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        val loggingParams = analyticsDataFactory.createParams(
            AnalyticsEvent.PaymentIntentRetrieve
        )
        assertThat(loggingParams)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, loggingParams[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            AnalyticsEvent.PaymentIntentRetrieve.toString(),
            loggingParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(
            AnalyticsDataFactory.ANALYTICS_UA,
            loggingParams[AnalyticsDataFactory.FIELD_ANALYTICS_UA]
        )
    }

    @Test
    fun getSetupIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val params = analyticsDataFactory.createSetupIntentConfirmationParams(
            PaymentMethod.Type.Card.code,
            "seti_12345"
        )
        assertEquals("card", params[AnalyticsDataFactory.FIELD_PAYMENT_METHOD_TYPE])
    }

    @Test
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.TokenCreate.toString()
        val expectedUaName = AnalyticsDataFactory.ANALYTICS_UA

        val versionCode = 20
        val packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        val packageInfo = PackageInfo().also {
            it.versionCode = versionCode
            it.packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        }

        val params =
            AnalyticsDataFactory(packageManager, packageInfo, packageName) { API_KEY }
                .createTokenCreationParams(
                    ATTRIBUTION,
                    Token.Type.Card
                )
        assertThat(params)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 1)
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(ATTRIBUTION.toList(), params[AnalyticsDataFactory.FIELD_PRODUCT_USAGE])
        assertEquals(Token.Type.Card.code, params[AnalyticsDataFactory.FIELD_TOKEN_TYPE])
        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])
        assertEquals(versionCode, params[AnalyticsDataFactory.FIELD_APP_VERSION])
        assertEquals(BuildConfig.LIBRARY_PACKAGE_NAME, params[AnalyticsDataFactory.FIELD_APP_NAME])

        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[AnalyticsDataFactory.FIELD_ANALYTICS_UA])

        assertEquals(
            "unknown_Android_robolectric",
            params[AnalyticsDataFactory.FIELD_DEVICE_TYPE]
        )
    }

    @Test
    fun getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = AnalyticsEvent.SourceCreate.toString()
        val expectedUaName = AnalyticsDataFactory.ANALYTICS_UA

        val params = analyticsDataFactory.createSourceCreationParams(
            Source.SourceType.SEPA_DEBIT
        )
        assertThat(params)
            .hasSize(AnalyticsDataFactory.VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, params[AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(Source.SourceType.SEPA_DEBIT, params[AnalyticsDataFactory.FIELD_SOURCE_TYPE])

        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsDataFactory.FIELD_OS_VERSION])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsDataFactory.FIELD_OS_NAME])

        assertEquals(BuildConfig.VERSION_NAME, params[AnalyticsDataFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[AnalyticsDataFactory.FIELD_ANALYTICS_UA])

        assertEquals("unknown_Android_robolectric", params[AnalyticsDataFactory.FIELD_DEVICE_TYPE])
    }

    @Test
    fun createAppDataParams_whenPackageNameIsEmpty_returnsEmptyMap() {
        assertThat(
            AnalyticsDataFactory(null, null, "") { API_KEY }
                .createAppDataParams()
        ).isEmpty()
    }

    @Test
    fun createAppDataParams_whenPackageInfoNotFound_returnsEmptyMap() {
        val packageName = "fake_package"
        assertThat(
            AnalyticsDataFactory(packageManager, null, packageName) { API_KEY }
                .createAppDataParams()
        ).isEmpty()
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

    @Test
    fun `create3ds2ChallengeParams with uiTypeCode '01' should create params with expected 3ds2_ui_type`() {
        assertThat(
            analyticsDataFactory.create3ds2ChallengeParams(
                AnalyticsEvent.Auth3ds2ChallengeCompleted,
                "pi_123",
                "01"
            )
        ).containsEntry("3ds2_ui_type", "text")
    }

    @Test
    fun `create3ds2ChallengeParams with uiTypeCode '99' should create params with expected 3ds2_ui_type`() {
        assertThat(
            analyticsDataFactory.create3ds2ChallengeParams(
                AnalyticsEvent.Auth3ds2ChallengeCompleted,
                "pi_123",
                "99"
            )
        ).containsEntry("3ds2_ui_type", "none")
    }

    private companion object {
        private const val API_KEY = "pk_abc123"
        private val ATTRIBUTION = setOf("CardInputView")
    }
}

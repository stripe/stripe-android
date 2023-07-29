package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsFields
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.HEADER_X_STRIPE_USER_AGENT
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class for [PaymentAnalyticsRequestFactory].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentAnalyticsRequestFactoryTest {
    private val packageManager = mock<PackageManager>()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        API_KEY
    )

    @Test
    fun getTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEvent = PaymentAnalyticsEvent.TokenCreate.toString()

        val params = analyticsRequestFactory.createTokenCreation(
            ATTRIBUTION,
            Token.Type.Pii
        ).params

        // Size is SIZE-1 because tokens don't have a source_type field
        assertThat(params)
            .hasSize(VALID_PARAM_FIELDS.size - 1)

        assertThat(params[AnalyticsFields.EVENT])
            .isEqualTo(expectedEvent)
        assertThat(params[PaymentAnalyticsRequestFactory.FIELD_TOKEN_TYPE])
            .isEqualTo(Token.Type.Pii.code)
    }

    @Test
    fun getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = PaymentAnalyticsEvent.TokenCreate.toString()

        val params = analyticsRequestFactory.createTokenCreation(
            ATTRIBUTION,
            Token.Type.CvcUpdate
        ).params

        // Size is SIZE-1 because tokens don't have a source_type field
        assertThat(params)
            .hasSize(VALID_PARAM_FIELDS.size - 1)
        assertEquals(expectedEventName, params[AnalyticsFields.EVENT])
        assertEquals(Token.Type.CvcUpdate.code, params[PaymentAnalyticsRequestFactory.FIELD_TOKEN_TYPE])
    }

    @Test
    fun getSourceCreationParams_withValidInput_createsCorrectMap() {
        val loggingParams = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.SEPA_DEBIT,
            ATTRIBUTION
        ).params

        // Size is SIZE-1 because tokens don't have a token_type field
        assertThat(loggingParams)
            .hasSize(VALID_PARAM_FIELDS.size - 1)

        assertEquals(
            Source.SourceType.SEPA_DEBIT,
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE]
        )
        assertEquals(API_KEY, loggingParams[AnalyticsFields.PUBLISHABLE_KEY])
        assertEquals(
            PaymentAnalyticsEvent.SourceCreate.toString(),
            loggingParams[AnalyticsFields.EVENT]
        )
        assertEquals(
            AnalyticsRequestFactory.ANALYTICS_UA,
            loggingParams[AnalyticsFields.ANALYTICS_UA]
        )
    }

    @Test
    fun getPaymentMethodCreationParams() {
        assertThat(
            analyticsRequestFactory
                .createPaymentMethodCreation(
                    PaymentMethod.Type.Card.code,
                    ATTRIBUTION
                ).params
        ).isEqualTo(
            mapOf(
                "analytics_ua" to "analytics.stripe_android-1.0",
                "event" to "stripe_android.payment_method_creation",
                "publishable_key" to "pk_abc123",
                "os_name" to "REL",
                "os_release" to "11",
                "os_version" to 30,
                "device_type" to "robolectric_robolectric_robolectric",
                "bindings_version" to StripeSdkVersion.VERSION_NAME,
                "app_name" to "com.stripe.android.test",
                "app_version" to 0,
                "product_usage" to ATTRIBUTION.toList(),
                "source_type" to "card",
                "is_development" to true,
                "session_id" to AnalyticsRequestFactory.sessionId,
                "network_type" to "2G",
            )
        )
    }

    @Test
    fun createPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val loggingParams =
            analyticsRequestFactory.createPaymentIntentConfirmation(
                PaymentMethod.Type.Card.code
            ).params

        assertThat(loggingParams)
            .hasSize(VALID_PARAM_FIELDS.size - 2)
        assertThat(loggingParams[AnalyticsFields.PUBLISHABLE_KEY])
            .isEqualTo(API_KEY)
        assertThat(loggingParams[AnalyticsFields.EVENT])
            .isEqualTo(PaymentAnalyticsEvent.PaymentIntentConfirm.toString())
        assertThat(loggingParams[AnalyticsFields.ANALYTICS_UA])
            .isEqualTo(AnalyticsRequestFactory.ANALYTICS_UA)
    }

    @Test
    fun getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        val loggingParams = analyticsRequestFactory.createRequest(
            PaymentAnalyticsEvent.PaymentIntentRetrieve
        ).params

        assertThat(loggingParams)
            .hasSize(VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, loggingParams[AnalyticsFields.PUBLISHABLE_KEY])
        assertEquals(
            PaymentAnalyticsEvent.PaymentIntentRetrieve.toString(),
            loggingParams[AnalyticsFields.EVENT]
        )
        assertEquals(
            AnalyticsRequestFactory.ANALYTICS_UA,
            loggingParams[AnalyticsFields.ANALYTICS_UA]
        )
    }

    @Test
    fun getSetupIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val params = analyticsRequestFactory.createSetupIntentConfirmation(
            PaymentMethod.Type.Card.code
        ).params

        assertThat(params[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE])
            .isEqualTo("card")
    }

    @Test
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = PaymentAnalyticsEvent.TokenCreate.toString()
        val expectedUaName = AnalyticsRequestFactory.ANALYTICS_UA

        val versionCode = 20
        val packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        val packageInfo = PackageInfo().also {
            it.versionCode = versionCode
            it.packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        }

        val factory = PaymentAnalyticsRequestFactory(
            packageManager = packageManager,
            packageInfo = packageInfo,
            packageName = packageName,
            publishableKeyProvider = { API_KEY },
            networkTypeProvider = { "5G" },
        )
        val params = factory.createTokenCreation(
            ATTRIBUTION,
            Token.Type.Card
        ).params

        assertThat(params)
            .hasSize(VALID_PARAM_FIELDS.size - 1)
        assertEquals(API_KEY, params[AnalyticsFields.PUBLISHABLE_KEY])
        assertEquals(ATTRIBUTION.toList(), params[PaymentAnalyticsRequestFactory.FIELD_PRODUCT_USAGE])
        assertEquals(Token.Type.Card.code, params[PaymentAnalyticsRequestFactory.FIELD_TOKEN_TYPE])
        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsFields.OS_VERSION])
        assertNotNull(params[AnalyticsFields.OS_RELEASE])
        assertNotNull(params[AnalyticsFields.OS_NAME])
        assertEquals(versionCode, params[AnalyticsFields.APP_VERSION])
        assertThat(params[AnalyticsFields.APP_NAME]).isEqualTo(BuildConfig.LIBRARY_PACKAGE_NAME)
        assertThat(params[AnalyticsFields.NETWORK_TYPE]).isEqualTo("5G")

        assertEquals(StripeSdkVersion.VERSION_NAME, params[AnalyticsFields.BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsFields.EVENT])
        assertEquals(expectedUaName, params[AnalyticsFields.ANALYTICS_UA])

        assertEquals(
            "robolectric_robolectric_robolectric",
            params[AnalyticsFields.DEVICE_TYPE]
        )
    }

    @Test
    fun getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = PaymentAnalyticsEvent.SourceCreate.toString()
        val expectedUaName = AnalyticsRequestFactory.ANALYTICS_UA

        val params = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.SEPA_DEBIT
        ).params

        assertThat(params)
            .hasSize(VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, params[AnalyticsFields.PUBLISHABLE_KEY])
        assertEquals(
            Source.SourceType.SEPA_DEBIT,
            params[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE]
        )

        assertEquals(Build.VERSION.SDK_INT, params[AnalyticsFields.OS_VERSION])
        assertNotNull(params[AnalyticsFields.OS_RELEASE])
        assertNotNull(params[AnalyticsFields.OS_NAME])

        assertEquals(StripeSdkVersion.VERSION_NAME, params[AnalyticsFields.BINDINGS_VERSION])
        assertEquals(expectedEventName, params[AnalyticsFields.EVENT])
        assertEquals(expectedUaName, params[AnalyticsFields.ANALYTICS_UA])

        assertEquals(
            "robolectric_robolectric_robolectric",
            params[AnalyticsFields.DEVICE_TYPE]
        )
    }

    @Test
    fun getEventParamName_withTokenCreation_createsExpectedParameter() {
        val expectedEventParam = "stripe_android.token_creation"
        assertEquals(
            expectedEventParam,
            PaymentAnalyticsEvent.TokenCreate.toString()
        )
    }

    @Test
    fun `create3ds2ChallengeParams with uiTypeCode '01' should create params with expected 3ds2_ui_type`() {
        assertThat(
            analyticsRequestFactory.create3ds2Challenge(
                PaymentAnalyticsEvent.Auth3ds2ChallengeCompleted,
                "01"
            ).params
        ).containsEntry("3ds2_ui_type", "text")
    }

    @Test
    fun `create3ds2ChallengeParams with uiTypeCode '99' should create params with expected 3ds2_ui_type`() {
        val params = analyticsRequestFactory.create3ds2Challenge(
            PaymentAnalyticsEvent.Auth3ds2ChallengeCompleted,
            "99"
        ).params

        assertThat(params)
            .containsEntry("3ds2_ui_type", "none")
    }

    @Test
    fun `create should create object with expected url and headers`() {
        val sdkVersion = StripeSdkVersion.VERSION_NAME
        val analyticsRequest = analyticsRequestFactory.createPaymentMethodCreation(
            PaymentMethod.Type.Card.code,
            emptySet()
        )
        assertThat(analyticsRequest.headers)
            .isEqualTo(
                mapOf(
                    "User-Agent" to "Stripe/v1 AndroidBindings/$sdkVersion",
                    "Accept-Charset" to "UTF-8",
                    HEADER_X_STRIPE_USER_AGENT to "{\"lang\":\"kotlin\"," +
                        "\"bindings_version\":\"${StripeSdkVersion.VERSION_NAME}\"," +
                        "\"os_version\":\"${Build.VERSION.SDK_INT}\"," +
                        "\"type\":\"robolectric_robolectric_robolectric\"," +
                        "\"model\":\"robolectric\"}"
                )
            )
        assertThat(analyticsRequest.url)
            .isEqualTo("https://q.stripe.com?publishable_key=pk_abc123&app_version=0&bindings_version=$sdkVersion&os_version=30&session_id=${AnalyticsRequestFactory.sessionId}&os_release=11&device_type=robolectric_robolectric_robolectric&source_type=card&app_name=com.stripe.android.test&analytics_ua=analytics.stripe_android-1.0&os_name=REL&network_type=2G&event=stripe_android.payment_method_creation&is_development=true")
    }

    @Test
    fun `product_usage param should include defaultProductUsageTokens and method argument`() {
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context,
            API_KEY,
            defaultProductUsageTokens = setOf("Hello")
        )

        val analyticsRequest = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.CARD,
            setOf("World")
        )

        val productUsage = analyticsRequest.params["product_usage"]
        assertThat(productUsage)
            .isEqualTo(listOf("Hello", "World"))
    }

    @Test
    fun `product_usage param should de-dupe defaultProductUsageTokens and method argument`() {
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context,
            API_KEY,
            defaultProductUsageTokens = setOf("Hello")
        )

        val analyticsRequest = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.CARD,
            setOf("Hello")
        )

        assertThat(analyticsRequest.params["product_usage"])
            .isEqualTo(listOf("Hello"))
    }

    @Test
    fun `product_usage param should use defaultProductUsageTokens`() {
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context,
            API_KEY,
            defaultProductUsageTokens = setOf("Hello")
        )

        val analyticsRequest = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.CARD
        )

        val productUsage = analyticsRequest.params["product_usage"]
        assertThat(productUsage)
            .isEqualTo(listOf("Hello"))
    }

    private companion object {
        private const val API_KEY = "pk_abc123"
        private val ATTRIBUTION = setOf("CardInputView")

        @JvmSynthetic
        private val VALID_PARAM_FIELDS: Set<String> = setOf(
            AnalyticsFields.ANALYTICS_UA,
            AnalyticsFields.APP_NAME,
            AnalyticsFields.APP_VERSION,
            AnalyticsFields.BINDINGS_VERSION,
            AnalyticsFields.IS_DEVELOPMENT,
            AnalyticsFields.DEVICE_TYPE,
            AnalyticsFields.EVENT,
            AnalyticsFields.OS_VERSION,
            AnalyticsFields.OS_NAME,
            AnalyticsFields.OS_RELEASE,
            AnalyticsFields.PUBLISHABLE_KEY,
            AnalyticsFields.SESSION_ID,
            PaymentAnalyticsRequestFactory.FIELD_PRODUCT_USAGE,
            PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE,
            PaymentAnalyticsRequestFactory.FIELD_TOKEN_TYPE,
            AnalyticsFields.NETWORK_TYPE,
        )
    }
}

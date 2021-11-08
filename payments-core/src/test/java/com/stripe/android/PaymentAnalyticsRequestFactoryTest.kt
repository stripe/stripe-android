package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.networking.ApiRequest
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
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 1)

        assertThat(params[PaymentAnalyticsRequestFactory.FIELD_EVENT])
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
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 1)
        assertEquals(expectedEventName, params[PaymentAnalyticsRequestFactory.FIELD_EVENT])
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
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 1)

        assertEquals(
            Source.SourceType.SEPA_DEBIT,
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE]
        )
        assertEquals(API_KEY, loggingParams[PaymentAnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            PaymentAnalyticsEvent.SourceCreate.toString(),
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_EVENT]
        )
        assertEquals(
            PaymentAnalyticsRequestFactory.ANALYTICS_UA,
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_ANALYTICS_UA]
        )
    }

    @Test
    fun getPaymentMethodCreationParams() {
        assertThat(
            analyticsRequestFactory
                .createPaymentMethodCreation(
                    PaymentMethod.Type.Card,
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
                "bindings_version" to Stripe.VERSION_NAME,
                "app_name" to "com.stripe.android.test",
                "app_version" to 0,
                "product_usage" to ATTRIBUTION.toList(),
                "source_type" to "card",
                "is_development" to true
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
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 2)
        assertThat(loggingParams[PaymentAnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
            .isEqualTo(API_KEY)
        assertThat(loggingParams[PaymentAnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(PaymentAnalyticsEvent.PaymentIntentConfirm.toString())
        assertThat(loggingParams[PaymentAnalyticsRequestFactory.FIELD_ANALYTICS_UA])
            .isEqualTo(PaymentAnalyticsRequestFactory.ANALYTICS_UA)
    }

    @Test
    fun getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        val loggingParams = analyticsRequestFactory.createRequest(
            PaymentAnalyticsEvent.PaymentIntentRetrieve
        ).params

        assertThat(loggingParams)
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, loggingParams[PaymentAnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            PaymentAnalyticsEvent.PaymentIntentRetrieve.toString(),
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_EVENT]
        )
        assertEquals(
            PaymentAnalyticsRequestFactory.ANALYTICS_UA,
            loggingParams[PaymentAnalyticsRequestFactory.FIELD_ANALYTICS_UA]
        )
    }

    @Test
    fun getSetupIntentConfirmationParams_withValidInput_createsCorrectMap() {
        val params = analyticsRequestFactory.createSetupIntentConfirmation(
            PaymentMethod.Type.Card.code,
        ).params

        assertThat(params[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE])
            .isEqualTo("card")
    }

    @Test
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = PaymentAnalyticsEvent.TokenCreate.toString()
        val expectedUaName = PaymentAnalyticsRequestFactory.ANALYTICS_UA

        val versionCode = 20
        val packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        val packageInfo = PackageInfo().also {
            it.versionCode = versionCode
            it.packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        }

        val factory = PaymentAnalyticsRequestFactory(
            packageManager,
            packageInfo,
            packageName,
            { API_KEY }
        )
        val params = factory.createTokenCreation(
            ATTRIBUTION,
            Token.Type.Card
        ).params

        assertThat(params)
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 1)
        assertEquals(API_KEY, params[PaymentAnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(ATTRIBUTION.toList(), params[PaymentAnalyticsRequestFactory.FIELD_PRODUCT_USAGE])
        assertEquals(Token.Type.Card.code, params[PaymentAnalyticsRequestFactory.FIELD_TOKEN_TYPE])
        assertEquals(Build.VERSION.SDK_INT, params[PaymentAnalyticsRequestFactory.FIELD_OS_VERSION])
        assertNotNull(params[PaymentAnalyticsRequestFactory.FIELD_OS_RELEASE])
        assertNotNull(params[PaymentAnalyticsRequestFactory.FIELD_OS_NAME])
        assertEquals(versionCode, params[PaymentAnalyticsRequestFactory.FIELD_APP_VERSION])
        assertThat(params[PaymentAnalyticsRequestFactory.FIELD_APP_NAME])
            .isEqualTo(BuildConfig.LIBRARY_PACKAGE_NAME)

        assertEquals(Stripe.VERSION_NAME, params[PaymentAnalyticsRequestFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[PaymentAnalyticsRequestFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[PaymentAnalyticsRequestFactory.FIELD_ANALYTICS_UA])

        assertEquals(
            "robolectric_robolectric_robolectric",
            params[PaymentAnalyticsRequestFactory.FIELD_DEVICE_TYPE]
        )
    }

    @Test
    fun getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        val expectedEventName = PaymentAnalyticsEvent.SourceCreate.toString()
        val expectedUaName = PaymentAnalyticsRequestFactory.ANALYTICS_UA

        val params = analyticsRequestFactory.createSourceCreation(
            Source.SourceType.SEPA_DEBIT
        ).params

        assertThat(params)
            .hasSize(PaymentAnalyticsRequestFactory.VALID_PARAM_FIELDS.size - 2)
        assertEquals(API_KEY, params[PaymentAnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
        assertEquals(
            Source.SourceType.SEPA_DEBIT,
            params[PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE]
        )

        assertEquals(Build.VERSION.SDK_INT, params[PaymentAnalyticsRequestFactory.FIELD_OS_VERSION])
        assertNotNull(params[PaymentAnalyticsRequestFactory.FIELD_OS_RELEASE])
        assertNotNull(params[PaymentAnalyticsRequestFactory.FIELD_OS_NAME])

        assertEquals(Stripe.VERSION_NAME, params[PaymentAnalyticsRequestFactory.FIELD_BINDINGS_VERSION])
        assertEquals(expectedEventName, params[PaymentAnalyticsRequestFactory.FIELD_EVENT])
        assertEquals(expectedUaName, params[PaymentAnalyticsRequestFactory.FIELD_ANALYTICS_UA])

        assertEquals(
            "robolectric_robolectric_robolectric",
            params[PaymentAnalyticsRequestFactory.FIELD_DEVICE_TYPE]
        )
    }

    @Test
    fun createAppDataParams_whenPackageNameIsEmpty_returnsEmptyMap() {
        val factory = PaymentAnalyticsRequestFactory(
            null,
            null,
            "",
            { API_KEY }
        )
        assertThat(factory.createAppDataParams())
            .isEmpty()
    }

    @Test
    fun createAppDataParams_whenPackageInfoNotFound_returnsEmptyMap() {
        val packageName = "fake_package"
        val factory = PaymentAnalyticsRequestFactory(
            packageManager,
            null,
            packageName,
            { API_KEY }
        )
        assertThat(factory.createAppDataParams())
            .isEmpty()
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
    fun getAnalyticsUa_returnsExpectedValue() {
        assertThat(PaymentAnalyticsRequestFactory.ANALYTICS_UA)
            .isEqualTo("analytics.stripe_android-1.0")
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
    fun `when publishable key is unavailable, create params with undefined key`() {
        val factory = PaymentAnalyticsRequestFactory(
            context,
            publishableKeyProvider = { throw RuntimeException() }
        )
        val params = factory.createRequest(PaymentAnalyticsEvent.SourceRetrieve).params

        assertThat(params["publishable_key"])
            .isEqualTo(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
    }

    @Test
    fun `create should create object with expected url and headers`() {
        val sdkVersion = Stripe.VERSION_NAME
        val analyticsRequest = analyticsRequestFactory.createPaymentMethodCreation(
            PaymentMethod.Type.Card,
            emptySet()
        )
        assertThat(analyticsRequest.headers)
            .isEqualTo(
                mapOf(
                    "User-Agent" to "Stripe/v1 AndroidBindings/$sdkVersion",
                    "Accept-Charset" to "UTF-8"
                )
            )
        assertThat(analyticsRequest.url)
            .isEqualTo("https://q.stripe.com?publishable_key=pk_abc123&app_version=0&bindings_version=$sdkVersion&os_version=30&os_release=11&device_type=robolectric_robolectric_robolectric&source_type=card&app_name=com.stripe.android.test&analytics_ua=analytics.stripe_android-1.0&os_name=REL&event=stripe_android.payment_method_creation&is_development=true")
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
    }
}

package com.stripe.android.core.networking

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.reactnative.ReactNativeAnalytics
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.version.StripeSdkVersion
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestFactoryTest : TestCase() {

    private val packageManager = mock<PackageManager>()
    private val packageName = "com.stripe.android.test"
    private val apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY

    private val mockEvent = object : AnalyticsEvent {
        override val eventName: String = "randomEvent"
    }

    @Test
    fun `when publishable key is unavailable, create params with undefined key`() {
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { "5G" },
        )

        val params = factory.createRequest(mockEvent, emptyMap(), publishableKey = null).params

        assertThat(params["publishable_key"])
            .isEqualTo(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
    }

    @Test
    fun `when publishable key is a user key, it is redacted`() {
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { "5G" },
        )

        val params = factory.createRequest(mockEvent, emptyMap(), publishableKey = "uk_123").params

        assertThat(params["publishable_key"])
            .isEqualTo("[REDACTED_LIVE_KEY]")
    }

    @Test
    fun getEventLoggingParams_withProductUsage_createsAllFields() {
        val expectedUaName = AnalyticsRequestFactory.ANALYTICS_UA

        val versionCode = 20
        val packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        val packageInfo = PackageInfo().also {
            it.versionCode = versionCode
            it.packageName = BuildConfig.LIBRARY_PACKAGE_NAME
        }

        val factory = AnalyticsRequestFactory(
            packageManager,
            packageInfo,
            packageName,
            { "5G" },
        )
        val params = factory.createRequest(mockEvent, emptyMap(), publishableKey = apiKey).params

        assertThat(apiKey).isEqualTo(params[AnalyticsFields.PUBLISHABLE_KEY])
        assertThat(Build.VERSION.SDK_INT).isEqualTo(params[AnalyticsFields.OS_VERSION])
        assertThat(versionCode).isEqualTo(params[AnalyticsFields.APP_VERSION])
        assertThat(params[AnalyticsFields.APP_NAME]).isEqualTo(BuildConfig.LIBRARY_PACKAGE_NAME)
        assertThat(StripeSdkVersion.VERSION_NAME).isEqualTo(params[AnalyticsFields.BINDINGS_VERSION])
        assertThat(mockEvent.eventName).isEqualTo(params[AnalyticsFields.EVENT])
        assertThat(expectedUaName).isEqualTo(params[AnalyticsFields.ANALYTICS_UA])
        assertThat("unknown_Android_robolectric").isEqualTo(params[AnalyticsFields.DEVICE_TYPE])
        assertNotNull(params[AnalyticsFields.OS_RELEASE])
        assertNotNull(params[AnalyticsFields.OS_NAME])
        assertNotNull(params[AnalyticsFields.SESSION_ID])
        assertNotNull(params[AnalyticsFields.TIMESTAMP])
        // Verify timestamp is in seconds, not milliseconds: seconds are ~billions, millis are ~trillions
        assertThat(params[AnalyticsFields.TIMESTAMP] as? Double).isLessThan(10_000_000_000.0)
    }

    @Test
    fun createAppDataParams_whenPackageInfoNotFound_returnsEmptyMap() {
        val packageName = "fake_package"
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { "5G" },
        )
        assertThat(factory.appDataParams()).isEmpty()
    }

    @Test
    fun createAppDataParams_whenPackageNameIsEmpty_returnsEmptyMap() {
        val factory = AnalyticsRequestFactory(
            null,
            null,
            "",
            { "5G" },
        )
        assertThat(factory.appDataParams()).isEmpty()
    }

    @Test
    fun getPluginType_returnsExpectedValue() {
        val factory = createFakeAnalyticsRequestFactory(
            pluginTypeProvider = { "react-native" }
        )

        val request = factory.createRequest(
            mockEvent,
            mapOf(),
            publishableKey = apiKey,
        )

        assertThat(request.params[AnalyticsFields.PLUGIN_TYPE])
            .isEqualTo("react-native")
    }

    @Test
    fun `Adds correct locale to request`() {
        val locales = listOf(Locale.US, Locale.CANADA, Locale.GERMANY)

        val factory = AnalyticsRequestFactory(
            packageManager = null,
            packageInfo = null,
            packageName = "",
            networkTypeProvider = { "5G" },
        )

        val event = object : AnalyticsEvent {
            override val eventName: String = "test_event"
        }

        for (locale in locales) {
            withLocale(locale) {
                val request = factory.createRequest(
                    event = event,
                    additionalParams = emptyMap(),
                    publishableKey = apiKey,
                )
                assertThat(request.params).containsEntry("locale", locale.toString())
            }
        }
    }

    @Test
    fun `react native fields are absent by default`() {
        val factory = createFakeAnalyticsRequestFactory()
        val params = factory.createRequest(mockEvent, emptyMap(), publishableKey = apiKey).params

        assertThat(params).doesNotContainKey(AnalyticsFields.REACT_NATIVE_IS_NEW_ARCHITECTURE)
        assertThat(params).doesNotContainKey(AnalyticsFields.REACT_NATIVE_VERSION)
    }

    @OptIn(ReactNativeSdkInternal::class)
    @Test
    fun `react native fields are included when set`() {
        ReactNativeAnalytics.isNewArchitecture = false
        ReactNativeAnalytics.reactNativeVersion = "0.75.3"

        try {
            val factory = createFakeAnalyticsRequestFactory()
            val params = factory.createRequest(mockEvent, emptyMap(), publishableKey = apiKey).params

            assertThat(params[AnalyticsFields.REACT_NATIVE_IS_NEW_ARCHITECTURE]).isEqualTo(false)
            assertThat(params[AnalyticsFields.REACT_NATIVE_VERSION]).isEqualTo("0.75.3")
        } finally {
            ReactNativeAnalytics.isNewArchitecture = null
            ReactNativeAnalytics.reactNativeVersion = null
        }
    }

    @Test
    fun `each request uses its supplied publishable key`() {
        val factory = createFakeAnalyticsRequestFactory()

        val first = factory.createRequest(mockEvent, emptyMap(), ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val second = factory.createRequest(mockEvent, emptyMap(), ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        assertThat(first.params[AnalyticsFields.PUBLISHABLE_KEY]).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(second.params[AnalyticsFields.PUBLISHABLE_KEY]).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    private fun createFakeAnalyticsRequestFactory(
        pluginTypeProvider: Provider<String?> = Provider { null }
    ): AnalyticsRequestFactory {
        return AnalyticsRequestFactory(
            mock(),
            null,
            "fake_package",
            { "5G" },
            pluginTypeProvider,
        )
    }
}

private fun withLocale(locale: Locale, block: () -> Unit) {
    val original = Locale.getDefault()
    Locale.setDefault(locale)
    block()
    Locale.setDefault(original)
}

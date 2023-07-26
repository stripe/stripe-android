package com.stripe.android.core.networking

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.version.StripeSdkVersion
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestFactoryTest : TestCase() {

    private val packageManager = mock<PackageManager>()
    private val packageName = "com.stripe.android.test"
    private val apiKey = "pk_abc123"

    private val mockEvent = object : AnalyticsEvent {
        override val eventName: String = "randomEvent"
    }

    @Test
    fun `when publishable key is unavailable, create params with undefined key`() {
        val exception = APIException(RuntimeException())
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { throw exception },
            { "5G" },
        )

        val params = factory.createRequest(mockEvent, emptyMap()).params

        assertThat(params["publishable_key"])
            .isEqualTo(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
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
            { apiKey },
            { "5G" },
        )

        val params = factory.createRequest(mockEvent, emptyMap()).params

        assertThat(params[AnalyticsFields.PUBLISHABLE_KEY]).isEqualTo(apiKey)
        assertThat(params[AnalyticsFields.OS_VERSION]).isEqualTo(Build.VERSION.SDK_INT.toString())
        assertThat(params[AnalyticsFields.APP_VERSION]).isEqualTo(versionCode.toString())
        assertThat(params[AnalyticsFields.APP_NAME]).isEqualTo(BuildConfig.LIBRARY_PACKAGE_NAME)
        assertThat(params[AnalyticsFields.BINDINGS_VERSION]).isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(params[AnalyticsFields.EVENT]).isEqualTo(mockEvent.eventName)
        assertThat(params[AnalyticsFields.ANALYTICS_UA]).isEqualTo(expectedUaName)
        assertThat(params[AnalyticsFields.DEVICE_TYPE]).isEqualTo("unknown_generic_x86_robolectric")

        assertNotNull(params[AnalyticsFields.OS_RELEASE])
        assertNotNull(params[AnalyticsFields.OS_NAME])
        assertNotNull(params[AnalyticsFields.SESSION_ID])
    }

    @Test
    fun createAppDataParams_whenPackageInfoNotFound_returnsEmptyMap() {
        val packageName = "fake_package"
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { apiKey },
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
            { apiKey },
            { "5G" },
        )
        assertThat(factory.appDataParams()).isEmpty()
    }

    @Test
    fun getAnalyticsUa_returnsExpectedValue() {
        assertThat(AnalyticsRequestFactory.ANALYTICS_UA)
            .isEqualTo("analytics.stripe_android-1.0")
    }
}

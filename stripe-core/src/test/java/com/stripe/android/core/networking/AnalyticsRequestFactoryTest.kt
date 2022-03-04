package com.stripe.android.core.networking

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.version.StripeSdkVersion
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestFactoryTest : TestCase() {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val packageManager = mock<PackageManager>()
    private val packageName = "com.stripe.android.test"
    private val apiKey = "pk_abc123"

    private val mockEvent = object : AnalyticsEvent {
        override val eventName: String = "randomEvent"
    }

    val factory = AnalyticsRequestFactory(
        context.applicationContext.packageManager,
        runCatching {
            context.applicationContext.packageManager.getPackageInfo(packageName, 0)
        }.getOrNull(),
        context.applicationContext.packageName.orEmpty(),
        { apiKey }
    )

    @Test
    fun `when publishable key is unavailable, create params with undefined key`() {
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { throw RuntimeException() }
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
            { apiKey }
        )
        val params = factory.createRequest(mockEvent, emptyMap()).params

        assertThat(apiKey)
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_PUBLISHABLE_KEY])
        assertThat(Build.VERSION.SDK_INT)
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_OS_VERSION])
        assertThat(versionCode)
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_APP_VERSION])
        assertThat(params[AnalyticsRequestFactory.FIELD_APP_NAME])
            .isEqualTo(BuildConfig.LIBRARY_PACKAGE_NAME)
        assertThat(StripeSdkVersion.VERSION_NAME)
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_BINDINGS_VERSION])
        assertThat(mockEvent.toString())
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_EVENT])
        assertThat(expectedUaName)
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_ANALYTICS_UA])
        assertThat("unknown_generic_x86_robolectric")
            .isEqualTo(params[AnalyticsRequestFactory.FIELD_DEVICE_TYPE])
        assertNotNull(params[AnalyticsRequestFactory.FIELD_OS_RELEASE])
        assertNotNull(params[AnalyticsRequestFactory.FIELD_OS_NAME])
    }

    @Test
    fun createAppDataParams_whenPackageInfoNotFound_returnsEmptyMap() {
        val packageName = "fake_package"
        val factory = AnalyticsRequestFactory(
            mock(),
            null,
            packageName,
            { apiKey }
        )
        assertThat(factory.appDataParams()).isEmpty()
    }

    @Test
    fun createAppDataParams_whenPackageNameIsEmpty_returnsEmptyMap() {
        val factory = AnalyticsRequestFactory(
            null,
            null,
            "",
            { apiKey }
        )
        assertThat(factory.appDataParams()).isEmpty()
    }

    @Test
    fun getAnalyticsUa_returnsExpectedValue() {
        assertThat(AnalyticsRequestFactory.ANALYTICS_UA)
            .isEqualTo("analytics.stripe_android-1.0")
    }
}

package com.stripe.android.core.networking

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.version.StripeSdkVersion
import java.util.UUID
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AnalyticsRequestFactory(
    private val packageManager: PackageManager?,
    private val packageInfo: PackageInfo?,
    private val packageName: String,
    private val publishableKeyProvider: Provider<String>,
    private val networkTypeProvider: Provider<String?>,
) {
    /**
     * Builds an Analytics request for the given [AnalyticsEvent],
     * including common params + event-specific params defined in [AnalyticsEvent.params]
     *
     * @param additionalParams any extra parameters that should be sent with this event.
     * Ensure this common parameters are not already included in [standardParams].
     *
     */
    fun createRequest(
        event: AnalyticsEvent,
        additionalParams: Map<String, Any?>
    ): AnalyticsRequest {
        return AnalyticsRequest(
            params = createParams(event) + additionalParams,
            headers = RequestHeadersFactory.Analytics.create()
        )
    }

    private fun createParams(
        event: AnalyticsEvent
    ): Map<String, Any> {
        return standardParams() + appDataParams() + event.params()
    }

    private fun AnalyticsEvent.params(): Map<String, String> {
        return mapOf(AnalyticsFields.EVENT to this.eventName)
    }

    private fun standardParams(): Map<String, Any> = mapOf(
        AnalyticsFields.ANALYTICS_UA to ANALYTICS_UA,
        AnalyticsFields.PUBLISHABLE_KEY to runCatching {
            publishableKeyProvider.get()
        }.getOrDefault(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY),
        AnalyticsFields.OS_NAME to Build.VERSION.CODENAME,
        AnalyticsFields.OS_RELEASE to Build.VERSION.RELEASE,
        AnalyticsFields.OS_VERSION to Build.VERSION.SDK_INT,
        AnalyticsFields.DEVICE_TYPE to DEVICE_TYPE,
        AnalyticsFields.BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
        AnalyticsFields.IS_DEVELOPMENT to BuildConfig.DEBUG,
        AnalyticsFields.SESSION_ID to sessionId,
    ) + networkType()

    private fun networkType(): Map<String, String> {
        val networkType = networkTypeProvider.get() ?: return emptyMap()
        return mapOf(AnalyticsFields.NETWORK_TYPE to networkType)
    }

    internal fun appDataParams(): Map<String, Any> {
        return when {
            packageManager != null && packageInfo != null -> {
                mapOf(
                    AnalyticsFields.APP_NAME to getAppName(packageInfo, packageManager),
                    AnalyticsFields.APP_VERSION to packageInfo.versionCode
                )
            }
            else -> emptyMap()
        }
    }

    private fun getAppName(
        packageInfo: PackageInfo?,
        packageManager: PackageManager
    ): CharSequence {
        return packageInfo?.applicationInfo?.loadLabel(packageManager).takeUnless {
            it.isNullOrBlank()
        } ?: packageName
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Volatile
        @VisibleForTesting
        var sessionId: UUID = UUID.randomUUID()
            private set

        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"

        fun regenerateSessionId() {
            sessionId = UUID.randomUUID()
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsEvent {
    /**
     * value that will be sent as [AnalyticsFields.EVENT] param.
     */
    val eventName: String
}

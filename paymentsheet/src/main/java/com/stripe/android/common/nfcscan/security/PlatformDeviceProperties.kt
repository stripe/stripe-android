package com.stripe.android.common.nfcscan.security

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import javax.inject.Inject

internal interface PlatformDeviceProperties {
    @Throws(IllegalArgumentException::class)
    fun getString(key: String): String?

    @Throws(IllegalArgumentException::class)
    fun getBoolean(key: String): Boolean? = getString(key)?.toBoolean()
}

/**
 * Uses the [Settings.Global] content provider to access global device properties.
 */
internal class GlobalSettingsDeviceProperties @Inject constructor(
    context: Context,
) : PlatformDeviceProperties {
    private val contentResolver: ContentResolver = context.contentResolver

    override fun getString(key: String): String? {
        return Settings.Global.getString(contentResolver, key)
    }

    override fun getBoolean(key: String): Boolean? {
        return getString(key)?.toInt()?.let { it != 0 }
    }
}

/**
 * Uses reflection to access system properties from `android.os.SystemProperties`. Some of these properties are meant
 * to only be read by and written by the system itself. In case these properties cannot be accessed via reflection,
 * property reads will all return `null`.
 */
internal class OsSettingsDeviceProperties @Inject constructor() : PlatformDeviceProperties {
    override fun getString(key: String): String? {
        return runCatchingAndGet { Class.forName("android.os.SystemProperties") }
            ?.runCatchingAndGet { getMethod("get", String::class.java) }
            ?.runCatchingAndGet { invoke(null, key) as String? }
            ?.takeUnless { it.isEmpty() }
    }

    override fun getBoolean(key: String): Boolean? {
        return getString(key)?.toBoolean()
    }

    private fun <R, T> R.runCatchingAndGet(
        throwableBlock: R.() -> T,
    ): T? = runCatching { throwableBlock() }.getOrNull()
}

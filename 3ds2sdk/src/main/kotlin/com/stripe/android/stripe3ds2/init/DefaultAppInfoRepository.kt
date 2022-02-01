package com.stripe.android.stripe3ds2.init

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * A [AppInfoRepository] that persists to [SharedPreferences].
 *
 * A new [AppInfo] will be generated if one is not available (i.e. new install), or if the app's
 * version was upgraded.
 */
internal class DefaultAppInfoRepository @VisibleForTesting internal constructor(
    private val store: Store,
    private val appVersion: Int,
    workContext: CoroutineContext
) : AppInfoRepository {

    constructor(
        context: Context,
        workContext: CoroutineContext
    ) : this(
        context,
        getAppVersion(context),
        workContext
    )

    internal constructor(
        context: Context,
        appVersion: Int,
        workContext: CoroutineContext
    ) : this(
        Store.Default(
            context,
            appVersion,
            workContext
        ),
        appVersion,
        workContext
    )

    init {
        CoroutineScope(workContext).launch {
            if (appVersion != store.get()?.version) {
                // init AppInfo if the current app version is not the same as the last known
                // app version
                initAppInfo()
            }
        }
    }

    /**
     * @return the stored [AppInfo] if it exists, or generates a new one
     */
    override suspend fun get(): AppInfo {
        return store.get() ?: initAppInfo()
    }

    private fun initAppInfo(): AppInfo {
        return AppInfo(
            UUID.randomUUID().toString(),
            appVersion
        ).also {
            store.save(it)
        }
    }

    interface Store {
        suspend fun get(): AppInfo?
        fun save(appInfo: AppInfo)

        class Default(
            context: Context,
            private val appVersion: Int,
            private val workContext: CoroutineContext
        ) : Store {
            private val sharedPrefs: SharedPreferences by lazy {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

            override suspend fun get(): AppInfo? = withContext(workContext) {
                val appVersion = sharedPrefs.getInt(KEY_APP_VERSION, 0)
                sharedPrefs.getString(KEY_SDK_APP_ID, null)?.let {
                    AppInfo(it, appVersion)
                }
            }

            override fun save(appInfo: AppInfo) {
                sharedPrefs
                    .edit()
                    .putInt(KEY_APP_VERSION, appVersion)
                    .putString(KEY_SDK_APP_ID, appInfo.sdkAppId)
                    .apply()
            }

            private companion object {
                private const val PREFS_NAME = "app_info"

                private const val KEY_APP_VERSION = "app_version"
                private const val KEY_SDK_APP_ID = "sdk_app_id"
            }
        }
    }

    private companion object {
        private const val INVALID_VERSION_CODE = -1

        @Suppress("DEPRECATION")
        private fun getAppVersion(context: Context): Int {
            return runCatching {
                val packageInfo =
                    context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionCode
            }.getOrDefault(INVALID_VERSION_CODE)
        }
    }
}

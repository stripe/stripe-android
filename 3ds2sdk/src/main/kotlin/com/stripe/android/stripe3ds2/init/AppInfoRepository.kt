package com.stripe.android.stripe3ds2.init

/**
 * An interface for classes that provides a [AppInfo].
 */
internal interface AppInfoRepository {
    suspend fun get(): AppInfo
}

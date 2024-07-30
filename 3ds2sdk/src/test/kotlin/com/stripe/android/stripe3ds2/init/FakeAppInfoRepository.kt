package com.stripe.android.stripe3ds2.init

import java.util.UUID

internal class FakeAppInfoRepository(
    sdkAppId: String = UUID.randomUUID().toString()
) : AppInfoRepository {
    private val appInfo = AppInfo(
        sdkAppId = sdkAppId,
        version = 50
    )

    override suspend fun get(): AppInfo = appInfo
}

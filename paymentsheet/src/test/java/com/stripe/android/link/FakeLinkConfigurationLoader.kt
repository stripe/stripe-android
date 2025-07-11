package com.stripe.android.link

internal class FakeLinkConfigurationLoader : LinkConfigurationLoader {
    var linkConfigurationResult: Result<LinkConfiguration> = Result.success(TestFactory.LINK_CONFIGURATION)

    override suspend fun load(configuration: LinkController.Configuration): Result<LinkConfiguration> {
        return linkConfigurationResult
    }
}

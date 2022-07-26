package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import androidx.annotation.RestrictTo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AsyncLpmResourceRepository] that loads all lpm resources from JSON asynchronously.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AsyncLpmResourceRepository @Inject constructor(
    private val resources: Resources
) : ResourceRepository<LpmRepository> {
    override suspend fun waitUntilLoaded() {
        getRepository().waitUntilLoaded()
    }

    override fun isLoaded(): Boolean {
        return getRepository().isLoaded()
    }

    override fun getRepository(): LpmRepository {
        return LpmRepository.getInstance(LpmRepository.LpmRepositoryArguments(resources))
    }
}

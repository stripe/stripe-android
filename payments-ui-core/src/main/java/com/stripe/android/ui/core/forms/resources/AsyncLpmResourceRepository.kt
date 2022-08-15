package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AsyncLpmResourceRepository] that loads all lpm resources from JSON asynchronously.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AsyncLpmResourceRepository @Inject constructor(
    private val lpmRepository: LpmRepository
) : ResourceRepository<LpmRepository> {
    override suspend fun waitUntilLoaded() {
        getRepository().waitUntilLoaded()
    }

    override fun isLoaded(): Boolean {
        return getRepository().isLoaded()
    }

    override fun getRepository(): LpmRepository {
        return lpmRepository
    }
}

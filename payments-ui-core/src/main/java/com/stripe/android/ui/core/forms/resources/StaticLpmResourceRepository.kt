package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo

/**
 * [StaticLpmResourceRepository] that receives lpm resources pre-loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StaticLpmResourceRepository(
    private val lpmRepository: LpmRepository =
        LpmRepository(LpmRepository.LpmRepositoryArguments(null))
) : ResourceRepository<LpmRepository> {
    override suspend fun waitUntilLoaded() {
        // Nothing to do since everything is pre-loaded
    }

    override fun isLoaded() = true

    override fun getRepository(): LpmRepository {
        return lpmRepository
    }
}

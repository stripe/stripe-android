package com.stripe.android.link.injection

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides

@Module
internal object LinkAccountManagerModule {

    @Volatile
    private var linkAccountManager: LinkAccountManager? = null

    @Provides
    fun provideLinkAccountManager(
        config: LinkConfiguration,
        linkRepository: LinkRepository,
        linkEventsReporter: LinkEventsReporter,
        errorReporter: ErrorReporter,
    ): LinkAccountManager {
        return linkAccountManager ?: synchronized(this) {
            linkAccountManager ?: DefaultLinkAccountManager(
                config = config,
                linkRepository = linkRepository,
                linkEventsReporter = linkEventsReporter,
                errorReporter = errorReporter
            ).also {
                this.linkAccountManager = it
            }
        }
    }
}

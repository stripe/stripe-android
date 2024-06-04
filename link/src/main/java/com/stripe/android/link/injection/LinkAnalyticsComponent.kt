package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkEventsReporter
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LinkAnalyticsScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Subcomponent(
    modules = [
        LinkAnalyticsModule::class,
    ]
)
@LinkAnalyticsScope
interface LinkAnalyticsComponent {
    val linkAnalyticsHelper: LinkAnalyticsHelper

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Subcomponent.Builder
    interface Builder {

        fun build(): LinkAnalyticsComponent
    }
}

@Module
internal interface LinkAnalyticsModule {
    @Binds
    @LinkAnalyticsScope
    fun bindLinkEventsReporter(linkEventsReporter: DefaultLinkEventsReporter): LinkEventsReporter
}

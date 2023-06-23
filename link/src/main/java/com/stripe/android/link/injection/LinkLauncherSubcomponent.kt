package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import dagger.Subcomponent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Subcomponent(
    modules = [
        LinkCommonModule::class,
    ]
)
@LinkScope
interface LinkLauncherSubcomponent {
    val linkAnalyticsHelper: LinkAnalyticsHelper

    @Subcomponent.Builder
    interface Builder {

        fun build(): LinkLauncherSubcomponent
    }
}

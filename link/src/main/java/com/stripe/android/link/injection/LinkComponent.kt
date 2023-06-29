package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LinkScope

/**
 * Component that holds the dependency graph for [LinkPaymentLauncher] and related classes used for
 * inline sign up, before Link is launched.
 */
@LinkScope
@Subcomponent(
    modules = [
        LinkModule::class,
    ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class LinkComponent {
    internal abstract val linkAccountManager: LinkAccountManager
    internal abstract val linkEventsReporter: LinkEventsReporter
    internal abstract val configuration: LinkConfiguration

    internal abstract fun inject(factory: InlineSignupViewModel.Factory)

    val injector: NonFallbackInjector = object : NonFallbackInjector {
        override fun inject(injectable: Injectable<*>) {
            when (injectable) {
                is InlineSignupViewModel.Factory ->
                    this@LinkComponent.inject(injectable)
                else -> {
                    throw IllegalArgumentException(
                        "invalid Injectable $injectable requested in $this"
                    )
                }
            }
        }
    }

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: LinkConfiguration): Builder

        fun build(): LinkComponent
    }
}

package com.stripe.android.payments.core.injection

import com.stripe.android.payments.core.authentication.DefaultIntentAuthenticatorRegistry
import dagger.Component
import javax.inject.Singleton

/**
 * [Component] for com.stripe.android.payments.core.authentication.
 *
 * It holds the dagger graph for [DefaultIntentAuthenticatorRegistry], with
 * more dependencies daggerized and a higher level [Component]s created, this class will be merged
 * into it.
 */
@Singleton
@Component(modules = [AuthenticationModule::class])
internal interface AuthenticationComponent {
    val registry: DefaultIntentAuthenticatorRegistry
}

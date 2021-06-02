package com.stripe.android.payments.core.injection

import com.stripe.android.payments.core.authentication.DefaultIntentAuthenticatorRegistry
import dagger.Component
import javax.inject.Scope

/**
 * Scope for for intent authentication.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthenticationScope

/**
 * [Component] for com.stripe.android.payments.core.authentication.
 *
 * It holds the dagger graph for [DefaultIntentAuthenticatorRegistry], with
 * more dependencies daggerized and a higher level [Component]s created, this class will be merged
 * into it.
 */
@AuthenticationScope
@Component(modules = [AuthenticationModule::class])
internal interface AuthenticationComponent {
    val registry: DefaultIntentAuthenticatorRegistry
}

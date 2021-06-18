package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import dagger.MapKey
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 * [Qualifier] for the multibinding map between [NextActionData] and [IntentAuthenticator].
 */
@Qualifier
annotation class IntentAuthenticatorMap

/**
 * [MapKey] for the [IntentAuthenticatorMap], encapsulating the [NextActionData] class type.
 */
@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntentAuthenticatorKey(val value: KClass<out NextActionData>)

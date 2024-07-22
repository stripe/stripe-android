package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.nextactionhandler.PaymentNextActionHandler
import dagger.MapKey
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 * [Qualifier] for the multibinding map between [NextActionData] and [PaymentNextActionHandler].
 */
@Qualifier
annotation class IntentNextActionHandlerMap

/**
 * [MapKey] for the [IntentNextActionHandlerMap], encapsulating the [NextActionData] class type.
 */
@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntentNextActionHandlerKey(val value: KClass<out NextActionData>)

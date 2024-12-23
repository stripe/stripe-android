package com.stripe.android.financialconnections.di

import javax.inject.Scope

/**
 * Scope annotation for bindings that should exist for the life of an activity, surviving configuration.
 *
 * Note: Dependencies scoped to this won't be shared across activities.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ActivityRetainedScope

package com.stripe.android.paymentsheet

/** Marks instrumentation tests that create sessions through the playground backend. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class RequiresPlaygroundBackend

package com.stripe.android.paymentsheet

/** Marks instrumentation tests that require a managed device with a software keyboard. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class RequiresIme

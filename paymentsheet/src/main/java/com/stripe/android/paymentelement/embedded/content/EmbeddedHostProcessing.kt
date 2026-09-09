package com.stripe.android.paymentelement.embedded.content

import javax.inject.Qualifier

/** Qualifies the host-provided processing state for Embedded's vertical payment method list. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class EmbeddedHostProcessing

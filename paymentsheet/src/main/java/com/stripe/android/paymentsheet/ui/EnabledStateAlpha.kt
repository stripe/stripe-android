package com.stripe.android.paymentsheet.ui

private const val ENABLED_ALPHA = 1f
private const val DISABLED_ALPHA = 0.6f

internal fun enabledStateAlpha(isEnabled: Boolean): Float =
    if (isEnabled) ENABLED_ALPHA else DISABLED_ALPHA

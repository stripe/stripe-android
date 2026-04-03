package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import javax.inject.Qualifier

/**
 * [Qualifier] for coroutine context used for UI.
 */
@Qualifier
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class UIContext

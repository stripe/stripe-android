package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import javax.inject.Qualifier

/**
 * [Qualifier] for coroutine scoped to the view model.
 */
@Qualifier
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ViewModelScope

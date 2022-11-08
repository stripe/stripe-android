@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.utils

import android.app.Application
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CreationExtras.requireApplication(): Application {
    return requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}

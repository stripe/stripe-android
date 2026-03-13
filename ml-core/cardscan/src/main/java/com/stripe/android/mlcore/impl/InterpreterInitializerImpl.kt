package com.stripe.android.mlcore.impl

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.mlcore.base.InterpreterInitializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InterpreterInitializerImpl : InterpreterInitializer {
    override fun initialize(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        onSuccess()
    }
}

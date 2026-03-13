package com.stripe.android.mlcore.impl

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import com.stripe.android.mlcore.base.InterpreterInitializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InterpreterInitializerImpl : InterpreterInitializer {
    override fun initialize(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        TfLite.initialize(context).also {
            it.addOnSuccessListener { onSuccess() }
            it.addOnFailureListener(onFailure)
        }.let { initializeTask ->
            Tasks.await(initializeTask)
        }
    }
}

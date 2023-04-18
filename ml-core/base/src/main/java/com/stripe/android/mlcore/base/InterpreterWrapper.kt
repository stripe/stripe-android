package com.stripe.android.mlcore.base

import androidx.annotation.RestrictTo

/**
 * Wrapper for TFLite interpreter API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InterpreterWrapper {
    fun runForMultipleInputsOutputs(
        inputs: Array<Any>,
        outputs: Map<Int, Any>
    )

    fun run(input: Any, output: Any)

    fun close()
}

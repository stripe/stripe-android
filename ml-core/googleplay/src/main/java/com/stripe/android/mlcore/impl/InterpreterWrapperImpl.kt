package com.stripe.android.mlcore.impl

import androidx.annotation.RestrictTo
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import org.tensorflow.lite.InterpreterApi
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InterpreterWrapperImpl(file: File, options: InterpreterOptionsWrapper) : InterpreterWrapper {
    private val interpreter: InterpreterApi =
        InterpreterApi.create(file, options.toInterpreterApiOptions())

    override fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>) {
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    override fun run(input: Any, output: Any) {
        interpreter.run(input, output)
    }
}

private fun InterpreterOptionsWrapper.toInterpreterApiOptions(): InterpreterApi.Options {
    val ret =
        InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
    useNNAPI?.let {
        ret.setUseNNAPI(it)
    }
    numThreads?.let {
        ret.setNumThreads(it)
    }
    return ret
}

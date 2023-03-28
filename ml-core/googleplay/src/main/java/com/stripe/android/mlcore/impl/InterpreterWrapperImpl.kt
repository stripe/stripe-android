package com.stripe.android.mlcore.impl

import androidx.annotation.RestrictTo
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import org.tensorflow.lite.InterpreterApi
import java.io.File
import java.nio.ByteBuffer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InterpreterWrapperImpl : InterpreterWrapper {
    private val interpreter: InterpreterApi

    constructor(byteBuffer: ByteBuffer, options: InterpreterOptionsWrapper) {
        interpreter = InterpreterApi.create(byteBuffer, options.toInterpreterApiOptions())
    }

    constructor(file: File, options: InterpreterOptionsWrapper) {
        interpreter = InterpreterApi.create(file, options.toInterpreterApiOptions())
    }

    override fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>) {
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    override fun run(input: Any, output: Any) {
        interpreter.run(input, output)
    }

    override fun close() {
        interpreter.close()
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

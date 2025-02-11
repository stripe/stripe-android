package com.stripe.android.mlcore.impl

import androidx.annotation.RestrictTo
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import org.tensorflow.lite.Interpreter
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InterpreterWrapperImpl constructor(file: File, options: InterpreterOptionsWrapper) : InterpreterWrapper {
    private val interpreter: Interpreter = Interpreter(file, options.toInterpreterOptions())

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

private fun InterpreterOptionsWrapper.toInterpreterOptions(): Interpreter.Options {
    val ret = Interpreter.Options()
    useNNAPI?.let {
        ret.setUseNNAPI(it)
    }
    numThreads?.let {
        ret.setNumThreads(it)
    }
    return ret
}

package com.stripe.android.stripecardscan.framework.ml

import android.content.Context
import android.util.Log
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import com.stripe.android.mlcore.impl.InterpreterWrapperImpl
import com.stripe.android.stripecardscan.framework.FetchedData
import com.stripe.android.stripecardscan.framework.Loader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * A TensorFlowLite analyzer uses an [InterpreterWrapper] to analyze data.
 */
internal abstract class TensorFlowLiteAnalyzer<Input, MLInput, Output, MLOutput>(
    private val tfInterpreter: InterpreterWrapper,
) : Analyzer<Input, Any, Output>, Closeable {

    protected abstract suspend fun interpretMLOutput(data: Input, mlOutput: MLOutput): Output

    protected abstract suspend fun transformData(data: Input): MLInput

    protected abstract suspend fun executeInference(
        tfInterpreter: InterpreterWrapper,
        data: MLInput
    ): MLOutput

    override suspend fun analyze(data: Input, state: Any): Output {
        val mlInput = transformData(data)

        val mlOutput = executeInference(tfInterpreter, mlInput)

        return interpretMLOutput(data, mlOutput)
    }

    override fun close() {
        tfInterpreter.close()
    }
}

/**
 * A factory that creates tensorflow models as analyzers.
 */
internal abstract class TFLAnalyzerFactory<
    Input,
    Output,
    AnalyzerType : Analyzer<Input, Any, Output>
    >(
    private val context: Context,
    private val fetchedModel: FetchedData
) : AnalyzerFactory<Input, Any, Output, AnalyzerType> {
    protected abstract val tfOptions: InterpreterOptionsWrapper

    private val loader by lazy { Loader(context) }

    private val loadModelMutex = Mutex()

    private var loadedModel: ByteBuffer? = null

//    protected suspend fun createInterpreter(): Interpreter? =
//        createInterpreter(fetchedModel)

    protected suspend fun createInterpreter(): InterpreterWrapper? =
        createInterpreter(fetchedModel)

    private suspend fun createInterpreter(fetchedModel: FetchedData): InterpreterWrapper? = try {
        loadModel(fetchedModel)?.let { InterpreterWrapperImpl(it, tfOptions) }
    } catch (t: Throwable) {
        Log.e(
            LOG_TAG,
            "Error loading ${fetchedModel.modelClass} version ${fetchedModel.modelVersion}",
            t
        )
        null
    }.apply {
        if (this == null) {
            Log.w(
                LOG_TAG,
                "Unable to load ${fetchedModel.modelClass} version ${fetchedModel.modelVersion}"
            )
        }
    }

    private suspend fun loadModel(fetchedModel: FetchedData): ByteBuffer? =
        loadModelMutex.withLock { loadedModel ?: run { loader.loadData(fetchedModel) } }

    companion object {
        private val LOG_TAG = TFLAnalyzerFactory::class.java.simpleName
    }
}

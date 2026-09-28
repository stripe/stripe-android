package com.stripe.android.identity.networking

import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.impl.InterpreterWrapperImpl
import java.io.File
import javax.inject.Inject

internal class DefaultIdentityModelFetcher @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val identityIO: IdentityIO,
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
) : IdentityModelFetcher {
    override suspend fun fetchIdentityModel(modelUrl: String): File {
        // Use the filename as a look up key
        identityIO.createTFLiteFile(modelUrl).let { tfliteFile ->
            return if (tfliteFile.exists() && validateModel(tfliteFile)) {
                tfliteFile
            } else {
                identityRepository.downloadModel(modelUrl).also {
                    if (!validateModel(tfliteFile)) {
                        throw IllegalStateException("Invalid TFLite model, likely a corrupted download")
                    }
                }
            }
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun validateModel(modelFile: File): Boolean {
        // Try to load the model file
        return try {
            InterpreterWrapperImpl(
                modelFile,
                InterpreterOptionsWrapper.Builder().build()
            )
            true
        } catch (e: Exception) {
            identityAnalyticsRequestFactory.genericError(
                throwable = e,
                overrideMessage = "Failed to validate TFLite model: ${modelFile.name}",
                additionalMetadata = mapOf(
                    IdentityAnalyticsRequestFactory.PARAM_ERROR_CONTEXT to
                        IdentityAnalyticsRequestFactory.ERROR_CONTEXT_MODEL_LOADING,
                    IdentityAnalyticsRequestFactory.PARAM_ML_MODEL_STAGE to
                        IdentityAnalyticsRequestFactory.MODEL_LOADING_STAGE_VALIDATE,
                    IdentityAnalyticsRequestFactory.PARAM_FILE_NAME to modelFile.name
                )
            )
            false
        }
    }
}

package com.stripe.android.identity.networking

import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.impl.InterpreterWrapperImpl
import java.io.File
import javax.inject.Inject

internal class DefaultIdentityModelFetcher @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val identityIO: IdentityIO
) : IdentityModelFetcher {
    override suspend fun fetchIdentityModel(modelUrl: String): File {
        // Use the filename as a look up key
        identityIO.createTFLiteFile(modelUrl).let { tfliteFile ->
            return if (tfliteFile.exists() && validateModel(tfliteFile)) {
                tfliteFile
            } else {
                identityRepository.downloadModel(modelUrl).also {
                    if (!validateModel(tfliteFile)) {
                        throw IllegalStateException("Downloaded model could not be loaded and failed validation")
                    }
                }
            }
        }
    }

    private fun validateModel(modelFile: File): Boolean {
        // Try to load the model file
        return try {
            InterpreterWrapperImpl(
                modelFile,
                InterpreterOptionsWrapper.Builder().build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}



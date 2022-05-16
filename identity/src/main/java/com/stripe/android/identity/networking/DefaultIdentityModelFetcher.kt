package com.stripe.android.identity.networking

import com.stripe.android.identity.utils.IdentityIO
import java.io.File
import javax.inject.Inject

internal class DefaultIdentityModelFetcher @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val identityIO: IdentityIO
) : IdentityModelFetcher {
    override suspend fun fetchIdentityModel(modelUrl: String): File {
        // Use the filename as a look up key
        identityIO.createTFLiteFile(modelUrl).let { tfliteFile ->
            return if (tfliteFile.exists()) {
                tfliteFile
            } else {
                identityRepository.downloadModel(modelUrl)
            }
        }
    }
}

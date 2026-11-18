package com.stripe.android.identity.viewmodel

import app.cash.turbine.Turbine
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.networking.IdentityRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/** Other repository operations retain the shared ViewModel test fixture's behavior. */
internal class FakeImageUploadRepository(
    delegate: IdentityRepository,
) : IdentityRepository by delegate {
    val uploadCalls = Turbine<UploadCall>()
    var result: Result<StripeFile> = Result.success(StripeFile())
    var uploadGate: CompletableDeferred<Unit>? = null
    var uploadGatesByFile: Map<File, CompletableDeferred<Unit>> = emptyMap()
    var finishUploadOnCancellation = false

    override suspend fun uploadImage(
        verificationId: String,
        ephemeralKey: String,
        imageFile: File,
        filePurpose: StripeFilePurpose,
        onSuccessExecutionTimeBlock: (Long) -> Unit
    ): StripeFile {
        val gate = uploadGatesByFile[imageFile] ?: uploadGate
        val uploadResult = result
        uploadCalls.add(UploadCall(imageFile, Thread.currentThread()))
        if (finishUploadOnCancellation) {
            withContext(NonCancellable) { gate?.await() }
        } else {
            gate?.await()
        }
        return uploadResult.getOrThrow()
    }

    fun ensureAllEventsConsumed() = uploadCalls.ensureAllEventsConsumed()

    data class UploadCall(val file: File, val thread: Thread)
}

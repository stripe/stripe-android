package com.stripe.android.identity.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import com.stripe.android.identity.example.ui.VerificationType
import kotlinx.serialization.json.Json

internal class IdentityExampleViewModel(application: Application) : AndroidViewModel(application) {
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    fun postForResult(
        type: VerificationType,
        allowDrivingLicense: Boolean,
        allowPassport: Boolean,
        allowId: Boolean,
        requireLiveCapture: Boolean,
        requireId: Boolean,
        requireSelfie: Boolean,
        requireAddress: Boolean
    ) = liveData {
        val result = Fuel.post(EXAMPLE_BACKEND_URL)
            .header("content-type", "application/json")
            .body(
                json.encodeToString(
                    VerificationSessionCreationRequest.serializer(),
                    VerificationSessionCreationRequest(
                        type = type.value,
                        options =
                        if (type == VerificationType.DOCUMENT) {
                            VerificationSessionCreationRequest.Options(
                                document = VerificationSessionCreationRequest.Document(
                                    requireIdNumber = requireId,
                                    requireMatchingSelfie = requireSelfie,
                                    requireLiveCapture = requireLiveCapture,
                                    requireAddress = requireAddress,
                                    allowedTypes = mutableListOf<String>().also {
                                        if (allowDrivingLicense) it.add(DRIVING_LICENSE)
                                        if (allowPassport) it.add(PASSPORT)
                                        if (allowId) it.add(ID_CARD)
                                    }
                                )
                            )
                        } else {
                            null
                        }
                    )
                )
            ).awaitStringResult()

        if (result is Result.Failure) {
            emit(result)
        } else {
            try {
                json.decodeFromString(
                    VerificationSessionCreationResponse.serializer(),
                    result.get()
                ).let {
                    emit(Result.success(it))
                }
            } catch (t: Throwable) {
                emit(Result.error(Exception(t)))
            }
        }
    }

    private companion object {
        const val DRIVING_LICENSE = "driving_license"
        const val PASSPORT = "passport"
        const val ID_CARD = "id_card"

        // TODO(ccen) revert to reflective-fossil-rib when id/address is supported
        const val EXAMPLE_BACKEND_URL =
            "https://humane-tidy-restaurant.glitch.me/create-verification-session"
//            "https://reflective-fossil-rib.glitch.me/create-verification-session"
    }
}

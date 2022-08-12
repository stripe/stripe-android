package com.stripe.android.identity.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import kotlinx.serialization.json.Json

internal class IdentityExampleViewModel : ViewModel() {
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    }

    fun postForResult(
        allowDrivingLicense: Boolean,
        allowPassport: Boolean,
        allowId: Boolean,
        requireLiveCapture: Boolean,
        requireId: Boolean,
        requireSelfie: Boolean
    ) = liveData {
        val result = Fuel.post(EXAMPLE_BACKEND_URL)
            .header("content-type", "application/json")
            .body(
                json.encodeToString(
                    VerificationSessionCreationRequest.serializer(),
                    VerificationSessionCreationRequest(
                        options = VerificationSessionCreationRequest.Options(
                            document = VerificationSessionCreationRequest.Document(
                                requireIdNumber = requireId,
                                requireMatchingSelfie = requireSelfie,
                                requireLiveCapture = requireLiveCapture,
                                allowedTypes = mutableListOf<String>().also {
                                    if (allowDrivingLicense) it.add(DRIVING_LICENSE)
                                    if (allowPassport) it.add(PASSPORT)
                                    if (allowId) it.add(ID_CARD)
                                }
                            )
                        )
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
        const val EXAMPLE_BACKEND_URL =
            "https://reflective-fossil-rib.glitch.me/create-verification-session"
    }
}

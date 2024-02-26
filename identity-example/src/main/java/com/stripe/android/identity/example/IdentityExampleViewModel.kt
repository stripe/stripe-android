package com.stripe.android.identity.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import com.stripe.android.identity.example.ui.IdentitySubmissionState
import com.stripe.android.identity.example.ui.VerificationType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

internal class IdentityExampleViewModel(application: Application) : AndroidViewModel(application) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    fun postForResult(
        submissionState: IdentitySubmissionState
    ) = liveData {
        val result = Fuel.post(EXAMPLE_BACKEND_URL)
            .header("content-type", "application/json")
            .body(
                json.encodeToString(
                    VerificationSessionCreationRequest.serializer(),
                    VerificationSessionCreationRequest(
                        type = submissionState.verificationType.value,
                        options =
                        when (submissionState.verificationType) {
                            VerificationType.DOCUMENT -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.Options(
                                        document = submissionState.toDocumentOptions(),
                                        phone = VerificationSessionCreationRequest.Options.Phone(
                                            requireVerification = true
                                        )
                                    )
                                } else {
                                    VerificationSessionCreationRequest.Options(
                                        document = submissionState.toDocumentOptions()
                                    )
                                }
                            }

                            VerificationType.PHONE -> {
                                if (submissionState.useDocumentFallback == true) {
                                    VerificationSessionCreationRequest.Options(
                                        document = submissionState.toDocumentOptions(),
                                        phoneRecords = VerificationSessionCreationRequest.Options.PhoneRecords(
                                            fallback = Fallback.Document
                                        ),
                                        phoneOtp = VerificationSessionCreationRequest.Options.PhoneOTP(
                                            check = requireNotNull(submissionState.phoneOtpCheck)
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.ID_NUMBER -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.Options(
                                        phone = VerificationSessionCreationRequest.Options.Phone(
                                            requireVerification = true
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.ADDRESS -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.Options(
                                        phone = VerificationSessionCreationRequest.Options.Phone(
                                            requireVerification = true
                                        )
                                    )
                                } else {
                                    null
                                }
                            }
                        },
                        providedDetails = when (submissionState.verificationType) {
                            VerificationType.DOCUMENT -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.ProvidedDetails(
                                        phone = requireNotNull(
                                            submissionState.providedPhoneNumber
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.ADDRESS -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.ProvidedDetails(
                                        phone = requireNotNull(
                                            submissionState.providedPhoneNumber
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.ID_NUMBER -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.ProvidedDetails(
                                        phone = requireNotNull(
                                            submissionState.providedPhoneNumber
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.PHONE -> {
                                null
                            }
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

    private fun IdentitySubmissionState.toDocumentOptions() =
        VerificationSessionCreationRequest.Options.Document(
            requireIdNumber = this.requireId,
            requireMatchingSelfie = this.requireSelfie,
            requireLiveCapture = this.requireLiveCapture,
            requireAddress = this.requireAddress,
            allowedTypes = mutableListOf<DocumentType>().also {
                if (this.allowDrivingLicense) {
                    it.add(
                        DocumentType.DrivingLicense
                    )
                }
                if (this.allowPassport) {
                    it.add(
                        DocumentType.Passport
                    )
                }
                if (this.allowId) it.add(DocumentType.IdCard)
            }
        )

    private companion object {
        const val EXAMPLE_BACKEND_URL =
            "https://stripe-mobile-identity-verification-playground.glitch.me/create-verification-session"
    }
}

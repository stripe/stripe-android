package com.stripe.android.identity.networking.models

import android.content.Context
import android.os.Parcelable
import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.R
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.ui.DRIVING_LICENSE_KEY
import com.stripe.android.identity.ui.ID_CARD_KEY
import com.stripe.android.identity.ui.PASSPORT_KEY
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@Parcelize
internal data class CollectedDataParam(
    @SerialName("biometric_consent")
    val biometricConsent: Boolean? = null,
    @SerialName("id_document_front")
    val idDocumentFront: DocumentUploadParam? = null,
    @SerialName("id_document_back")
    val idDocumentBack: DocumentUploadParam? = null,
    @SerialName("face")
    val face: FaceUploadParam? = null,
    @SerialName("id_number")
    val idNumber: IdNumberParam? = null,
    @SerialName("dob")
    val dob: DobParam? = null,
    @SerialName("name")
    val name: NameParam? = null,
    @SerialName("address")
    val address: RequiredInternationalAddress? = null,
    @SerialName("phone")
    val phone: PhoneParam? = null,
    @SerialName("phone_otp")
    val phoneOtp: String? = null
) : Parcelable {
    @Serializable
    internal enum class Type {
        @SerialName("driving_license")
        DRIVINGLICENSE,

        @SerialName("id_card")
        IDCARD,

        @SerialName("passport")
        PASSPORT,

        INVALID;

        companion object {
            fun fromName(typeName: String) =
                when (typeName) {
                    PASSPORT_KEY -> PASSPORT
                    DRIVING_LICENSE_KEY -> DRIVINGLICENSE
                    ID_CARD_KEY -> IDCARD
                    else -> {
                        throw IllegalArgumentException("Unknown name $typeName")
                    }
                }
        }
    }

    internal companion object {
        private const val COLLECTED_DATA_PARAM = "collected_data"

        /**
         * Create map entry for encoding into x-www-url-encoded string.
         */
        fun CollectedDataParam.createCollectedDataParamEntry(json: Json) =
            COLLECTED_DATA_PARAM to json.encodeToJsonElement(
                serializer(),
                this
            ).toMap()

        fun createFromFrontUploadedResultsForAutoCapture(
            frontHighResResult: UploadedResult,
            frontLowResResult: UploadedResult
        ): CollectedDataParam =
            CollectedDataParam(
                idDocumentFront = DocumentUploadParam(
                    backScore = requireNotNull(frontHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                    frontCardScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                    invalidScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                    passportScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_PASSPORT],
                    highResImage = requireNotNull(
                        frontHighResResult.uploadedStripeFile.id
                    ) {
                        "front high res image id is null"
                    },
                    lowResImage = requireNotNull(
                        frontLowResResult.uploadedStripeFile.id
                    ) {
                        "front low res image id is null"
                    },
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                )
            )

        fun createFromBackUploadedResultsForAutoCapture(
            backHighResResult: UploadedResult,
            backLowResResult: UploadedResult
        ): CollectedDataParam =
            CollectedDataParam(
                idDocumentBack = DocumentUploadParam(
                    backScore = requireNotNull(backHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                    frontCardScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                    invalidScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                    passportScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_PASSPORT],
                    highResImage = requireNotNull(
                        backHighResResult.uploadedStripeFile.id
                    ) {
                        "back high res image id is null"
                    },
                    lowResImage = requireNotNull(
                        backLowResResult.uploadedStripeFile.id
                    ) {
                        "back low res image id is null"
                    },
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                )
            )

        fun createForSelfie(
            firstHighResResult: UploadedResult,
            firstLowResResult: UploadedResult,
            lastHighResResult: UploadedResult,
            lastLowResResult: UploadedResult,
            bestHighResResult: UploadedResult,
            bestLowResResult: UploadedResult,
            trainingConsent: Boolean,
            bestFaceScore: Float,
            faceScoreVariance: Float,
            numFrames: Int
        ) = CollectedDataParam(
            face = FaceUploadParam(
                bestHighResImage = requireNotNull(bestHighResResult.uploadedStripeFile.id),
                bestLowResImage = requireNotNull(bestLowResResult.uploadedStripeFile.id),
                firstHighResImage = requireNotNull(firstHighResResult.uploadedStripeFile.id),
                firstLowResImage = requireNotNull(firstLowResResult.uploadedStripeFile.id),
                lastHighResImage = requireNotNull(lastHighResResult.uploadedStripeFile.id),
                lastLowResImage = requireNotNull(lastLowResResult.uploadedStripeFile.id),
                bestFaceScore = bestFaceScore,
                faceScoreVariance = faceScoreVariance,
                numFrames = numFrames,
                trainingConsent = trainingConsent
            )
        )

        fun CollectedDataParam.mergeWith(another: CollectedDataParam?): CollectedDataParam {
            return another?.let {
                this.copy(
                    biometricConsent = another.biometricConsent ?: this.biometricConsent,
                    idDocumentFront = another.idDocumentFront ?: this.idDocumentFront,
                    idDocumentBack = another.idDocumentBack ?: this.idDocumentBack,
                    face = another.face ?: this.face,
                    idNumber = another.idNumber ?: this.idNumber,
                    dob = another.dob ?: this.dob,
                    name = another.name ?: this.name,
                    address = another.address ?: this.address,
                    phone = another.phone ?: this.phone,
                    phoneOtp = another.phoneOtp ?: this.phoneOtp,
                )
            } ?: this
        }

        fun CollectedDataParam.clearData(field: Requirement): CollectedDataParam {
            return when (field) {
                Requirement.BIOMETRICCONSENT -> this.copy(biometricConsent = null)
                Requirement.IDDOCUMENTBACK -> this.copy(idDocumentBack = null)
                Requirement.IDDOCUMENTFRONT -> this.copy(idDocumentFront = null)
                Requirement.FACE -> this.copy(face = null)
                Requirement.IDNUMBER -> this.copy(idNumber = null)
                Requirement.DOB -> this.copy(dob = null)
                Requirement.NAME -> this.copy(name = null)
                Requirement.ADDRESS -> this.copy(address = null)
                Requirement.PHONE_NUMBER -> this.copy(phone = null)
                Requirement.PHONE_OTP -> this.copy(phoneOtp = null)
            }
        }

        fun CollectedDataParam.collectedRequirements(): Set<Requirement> {
            val requirements = mutableSetOf<Requirement>()
            this.biometricConsent?.let {
                requirements.add(Requirement.BIOMETRICCONSENT)
            }
            this.idDocumentFront?.let {
                requirements.add(Requirement.IDDOCUMENTFRONT)
            }
            this.idDocumentBack?.let {
                requirements.add(Requirement.IDDOCUMENTBACK)
            }
            this.face?.let {
                requirements.add(Requirement.FACE)
            }
            this.name?.let {
                requirements.add(Requirement.NAME)
            }
            this.dob?.let {
                requirements.add(Requirement.DOB)
            }
            this.idNumber?.let {
                requirements.add(Requirement.IDNUMBER)
            }
            this.address?.let {
                requirements.add(Requirement.ADDRESS)
            }
            this.phone?.let {
                requirements.add(Requirement.PHONE_NUMBER)
            }
            this.phoneOtp?.let {
                requirements.add(Requirement.PHONE_OTP)
            }
            return requirements
        }

        fun Type.getDisplayName(context: Context) =
            when (this) {
                Type.IDCARD -> {
                    context.getString(R.string.stripe_id_card)
                }

                Type.DRIVINGLICENSE -> {
                    context.getString(R.string.stripe_driver_license)
                }

                Type.PASSPORT -> {
                    context.getString(R.string.stripe_passport)
                }

                else -> throw java.lang.IllegalStateException("Invalid CollectedDataParam.Type")
            }
    }
}

package com.stripe.android.identity.networking.models

import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.networking.UploadedResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CollectedDataParam(
    @SerialName("biometric_consent")
    val biometricConsent: Boolean = true,
    @SerialName("id_document_type")
    val idDocumentType: Type? = null,
    @SerialName("id_document_front")
    val idDocumentFront: DocumentUploadParam? = null,
    @SerialName("id_document_back")
    val idDocumentBack: DocumentUploadParam? = null,
    // TODO(IDPROD-3944) - verify with server change
    @SerialName("training_consent")
    val trainingConsent: Boolean? = null,
    @SerialName("face")
    val face: FaceUploadParam? = null
) {
    @Serializable
    internal enum class Type {
        @SerialName("driving_license")
        DRIVINGLICENSE,

        @SerialName("id_card")
        IDCARD,

        @SerialName("passport")
        PASSPORT
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

        fun createFromUploadedResultsForAutoCapture(
            type: Type,
            frontHighResResult: UploadedResult,
            frontLowResResult: UploadedResult,
            backHighResResult: UploadedResult? = null,
            backLowResResult: UploadedResult? = null
        ): CollectedDataParam =
            if (backHighResResult != null && backLowResResult != null) {
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
                    ),
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
                    ),
                    idDocumentType = type
                )
            } else
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
                    ),
                    idDocumentType = type
                )

        fun createForSelfie(
            firstHighResResult: UploadedResult,
            firstLowResResult: UploadedResult,
            lastHighResResult: UploadedResult,
            lastLowResResult: UploadedResult,
            bestHighResResult: UploadedResult,
            bestLowResResult: UploadedResult,
            trainingConsent: Boolean,
            faceScoreVariance: Float,
            numFrames: Int
        ) = CollectedDataParam(
            trainingConsent = trainingConsent,
            face = FaceUploadParam(
                bestHighResImage = bestHighResResult.uploadedStripeFile.id,
                bestLowResImage = bestLowResResult.uploadedStripeFile.id,
                firstHighResImage = firstHighResResult.uploadedStripeFile.id,
                firstLowResImage = firstLowResResult.uploadedStripeFile.id,
                lastHighResImage = lastHighResResult.uploadedStripeFile.id,
                lastLowResImage = lastLowResResult.uploadedStripeFile.id,
                faceScoreVariance = faceScoreVariance,
                numFrames = numFrames
            )
        )
    }
}

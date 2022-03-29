package com.stripe.android.identity.networking.models

import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CollectedDataParam(
    @SerialName("consent")
    val consent: ConsentParam? = null,

    @SerialName("id_document")
    val idDocument: IdDocumentParam? = null
) {
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
            type: IdDocumentParam.Type,
            frontHighResResult: IdentityViewModel.UploadedResult,
            frontLowResResult: IdentityViewModel.UploadedResult,
            backHighResResult: IdentityViewModel.UploadedResult? = null,
            backLowResResult: IdentityViewModel.UploadedResult? = null
        ): CollectedDataParam =
            if (backHighResResult != null && backLowResResult != null) {
                CollectedDataParam(
                    idDocument = IdDocumentParam(
                        front = DocumentUploadParam(
                            backScore = requireNotNull(frontHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                            frontCardScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                            invalidScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                            noDocumentScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_NO_ID],
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
                        back = DocumentUploadParam(
                            backScore = requireNotNull(backHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                            frontCardScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                            invalidScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                            noDocumentScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_NO_ID],
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
                        type = type
                    )
                )
            } else
                CollectedDataParam(
                    idDocument = IdDocumentParam(
                        front = DocumentUploadParam(
                            backScore = requireNotNull(frontHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                            frontCardScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                            invalidScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                            noDocumentScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_NO_ID],
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
                        type = type
                    )
                )
    }
}

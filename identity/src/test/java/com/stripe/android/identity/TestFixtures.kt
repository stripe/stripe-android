package com.stripe.android.identity

import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import kotlinx.serialization.json.Json

internal const val ERROR_BODY = "errorBody"
internal const val ERROR_BUTTON_TEXT = "error button text"
internal const val ERROR_TITLE = "errorTitle"

internal val CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = emptyList(),
        missing = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = false
)

internal val CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = emptyList(),
        missing = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = true
)

internal val ERROR_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = listOf(
            VerificationPageDataRequirementError(
                body = ERROR_BODY,
                buttonText = ERROR_BUTTON_TEXT,
                requirement = VerificationPageDataRequirementError.Requirement.BIOMETRICCONSENT,
                title = ERROR_TITLE
            )
        ),
        missing = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = false
)

internal val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
internal val SUCCESS_VERIFICATION_PAGE: VerificationPage = json.decodeFromString(
    VerificationPage.serializer(),
    """
        {
          "id": "vs_1KgNstEAjaOkiuGMpFXVTocU",
          "object": "identity.verification_page",
          "biometric_consent": {
            "accept_button_text": "Accept and continue",
            "body": "\u003Cp\u003E\u003Cb\u003EHow Stripe will verify your identity\u003C/b\u003E\u003C/p\u003E\u003Cp\u003EStripe will use biometric technology (on images of you and your IDs) and other data sources to confirm your identity and for fraud and security purposes. Stripe will store these images and the results of this check and share them with mlgb.band.\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/about'\u003ELearn about Stripe\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy-center/legal#stripe-identity'\u003ELearn how Stripe Identity works\u003C/a\u003E\u003C/p\u003E",
            "decline_button_text": "No, don't verify",
            "privacy_policy": "Data will be stored and may be used according to the \u003Ca href='https://stripe.com/privacy'\u003EStripe Privacy Policy\u003C/a\u003E and mlgb.band Privacy Policy.",
            "time_estimate": "Takes about 1–2 minutes.",
            "title": "mlgb.band uses Stripe to verify your identity"
          },
          "document_capture": {
            "autocapture_timeout": 8000,
            "file_purpose": "identity_private",
            "high_res_image_compression_quality": 0.92,
            "high_res_image_crop_padding": 0.08,
            "high_res_image_max_dimension": 3000,
            "low_res_image_compression_quality": 0.82,
            "low_res_image_max_dimension": 3000,
            "models": {
              "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/945cd2bb8681a56dd5b0344a009a1f2416619382/assets/id_detectors/tflite/2022-02-23/model.tflite"
            },
            "require_live_capture": false
          },
          "document_select": {
            "body": null,
            "button_text": "Next",
            "id_document_type_allowlist": {
              "passport": "Passport",
              "driving_license": "Driver's license",
              "id_card": "Identity card"
            },
            "title": "Which form of identification do you want to use?"
          },
          "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xSU84aDNFQWphT2tpdUdNLF9MTjg5dFZtRWV1T1c1QXBxMkJ6MTUwZlI5c3JtTE5U0100BzkFlqqD",
          "livemode": false,
          "requirements": {
            "missing": [
              "biometric_consent",
              "id_document_front",
              "id_document_back",
              "id_document_type"
            ]
          },
          "status": "requires_input",
          "submitted": false,
          "success": {
            "body": "\u003Cp\u003E\u003Cb\u003EThank you for providing your information\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will reach out if additional details are required.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003ENext steps\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will contact you regarding the outcome of your identification process.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003EMore about Stripe Identity\u003C/b\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://support.stripe.com/questions/common-questions-about-stripe-identity'\u003ECommon questions about Stripe Identity\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy-center/legal#stripe-identity'\u003ELearn how Stripe uses data\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy'\u003EStripe Privacy Policy\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='mailto:privacy@stripe.com'\u003EContact Stripe\u003C/a\u003E\u003C/p\u003E",
            "button_text": "Complete",
            "title": "Verification pending"
          },
          "unsupported_client": false
        }
    """.trimIndent()
)

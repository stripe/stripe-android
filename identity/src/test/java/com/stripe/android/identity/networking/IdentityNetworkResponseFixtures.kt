package com.stripe.android.identity.networking

internal val VERIFICATION_PAGE_DATA_JSON_STRING = """
    {
      "id": "vs_1KWvnMEAjaOkiuGMvfNAA0vo",
      "object": "identity.verification_page_data",
      "requirements": {
        "errors": [

        ],
        "missing": [
          "id_document_front",
          "id_document_back",
          "id_document_type"
        ]
      },
      "status": "requires_input",
      "submitted": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_JSON_STRING = """
    {
      "id": "vs_1KYh7GEAjaOkiuGMG0cfqozD",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Accept and continue",
        "body": "Stripe will confirm your identity using biometric technology that uses images of you and your identification, and other data sources. The results will be shared with mlgb.band.",
        "decline_button_text": "No, don't verify",
        "title": "How Stripe will verify your identity"
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
          "id_detector_url": "https://b.stripecdn.com/gelato/assets/50e98374a70b71b2ee7ec8c3060f187ee1d833bd/assets/id_detectors/tflite/2022-02-23/model.tflite"
        },
        "require_live_capture": false
      },
      "document_select": {
        "button_text": "Next",
        "id_document_type_allowlist": {
          "passport": "Passport",
          "driving_license": "Driver's license",
          "id_card": "Identity card"
        },
        "title": "Select identification type"
      },
      "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xSU84aDNFQWphT2tpdUdNLF9MRkJVekZZTEJzYTNoRnowV3JqODZXeFJCQ2lJT3lU0100mZgbAr34",
      "livemode": false,
      "requirements": {
        "missing": [
          "id_document_front",
          "id_document_back",
          "id_document_type"
        ]
      },
      "status": "requires_input",
      "submitted": false,
      "success": {
        "body": "Thank you for providing your information. mlgb.band will reach out if additional details are required.",
        "button_text": "Done",
        "title": "Verification submitted"
      },
      "unsupported_client": false,
      "welcome": {
        "body": "Data will be stored and may be used according to the Stripe [Privacy Policy](https://stripe.com/privacy) and the mlgb.band [Privacy Policy](]). [Learn more](https://stripe.com/privacy-center/legal#stripe-identity).",
        "button_text": "Get started",
        "title": "mlgb.band partners with Stripe for secure identity verification"
      }
    }
""".trimIndent()

internal val ERROR_JSON_STRING = """
    {
      "error": {
        "code": "resource_missing",
        "doc_url": "https://stripe.com/docs/error-codes/resource-missing",
        "message": "No such file upload: 'hightResImage'",
        "param": "collected_data[id_document][front][high_res_image]",
        "type": "invalid_request_error"
      }
    }

""".trimIndent()

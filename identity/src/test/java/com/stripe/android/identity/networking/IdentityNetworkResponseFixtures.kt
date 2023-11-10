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
          "id_document_back"
        ]
      },
      "status": "requires_input",
      "submitted": false,
      "closed": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING = """
    {
      "id": "vs_1KgNstEAjaOkiuGMpFXVTocU",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "bottomsheet": {
        "consent_identity": {
          "bottomsheet_id": "consent_identity",
          "lines": [
            {
              "content": "We leverage Stripe's own verification service to verify your identity through your document and selfie.",
              "icon": "cloud",
              "title": "Stripe technology"
            },
            {
              "content": "We also work with trusted partners, including document issuers and authorized record holders, to help us verify your identity.",
              "icon": "moved",
              "title": "Third party partners"
            }
          ],
          "title": "How we verify you"
        },
        "consent_photo_id": {
          "bottomsheet_id": "consent_photo_id",
          "lines": [
            {
              "content": "<ul><li>Drivers license</li><li>Passport</li><li>National ID</li><li>Valid government-issued identification that clearly shows your face</li></ul>",
              "icon": "wallet",
              "title": "Accepted forms of identification"
            }
          ],
          "title": "Types of photo ID"
        },
        "consent_verification_data": {
          "bottomsheet_id": "consent_verification_data",
          "lines": [
            {
              "content": "Stripe handles billions of dollars in payments annually. The same infrastructure keeps identity verification data safe as well.",
              "icon": "lock",
              "title": "Your data is encrypted"
            },
            {
              "content": "Stripe will use and store your data under Stripe’s privacy policy, including to manage loss and for legal compliance.  <a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn more</a>.",
              "icon": "lock",
              "title": "Stripe data use"
            },
            {
              "content": "Andrew's Audio will have access to the information you submit and the status of your verification, and may use your information under its privacy policy.",
              "icon": "moved",
              "title": "Andrew's Audio access"
            },
            {
              "content": "You can delete your data by contacting Andrew's Audio.",
              "icon": "document",
              "title": "Manage your data"
            }
          ],
          "title": "Your data"
        }
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
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
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.8,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/945cd2bb8681a56dd5b0344a009a1f2416619382/assets/id_detectors/tflite/2022-02-23/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
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
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "biometric_consent",
          "id_document_front",
          "id_document_back"
        ]
      },
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "\u003Cp\u003E\u003Cb\u003EThank you for providing your information\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will reach out if additional details are required.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003ENext steps\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will contact you regarding the outcome of your identification process.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003EMore about Stripe Identity\u003C/b\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://support.stripe.com/questions/common-questions-about-stripe-identity'\u003ECommon questions about Stripe Identity\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy-center/legal#stripe-identity'\u003ELearn how Stripe uses data\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy'\u003EStripe Privacy Policy\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='mailto:privacy@stripe.com'\u003EContact Stripe\u003C/a\u003E\u003C/p\u003E",
        "button_text": "Complete",
        "title": "Verification pending"
      },
      "unsupported_client": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE_JSON_STRING = """
    {
      "id": "vs_1KgNstEAjaOkiuGMpFXVTocU",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
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
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.8,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/945cd2bb8681a56dd5b0344a009a1f2416619382/assets/id_detectors/tflite/2022-02-23/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": true
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
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "biometric_consent",
          "id_document_front",
          "id_document_back"
        ]
      },
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "\u003Cp\u003E\u003Cb\u003EThank you for providing your information\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will reach out if additional details are required.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003ENext steps\u003C/b\u003E\u003C/p\u003E\u003Cp\u003Emlgb.band will contact you regarding the outcome of your identification process.\u003C/p\u003E\u003Cp\u003E\u003Cb\u003EMore about Stripe Identity\u003C/b\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://support.stripe.com/questions/common-questions-about-stripe-identity'\u003ECommon questions about Stripe Identity\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy-center/legal#stripe-identity'\u003ELearn how Stripe uses data\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='https://stripe.com/privacy'\u003EStripe Privacy Policy\u003C/a\u003E\u003C/p\u003E\u003Cp\u003E\u003Ca href='mailto:privacy@stripe.com'\u003EContact Stripe\u003C/a\u003E\u003C/p\u003E",
        "button_text": "Complete",
        "title": "Verification pending"
      },
      "unsupported_client": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE_JSON_STRING = """
    {
      "id": "vs_1M8UU5GMZYGNxJkBN55D3nva",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": true
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/live_YWNjdF8xSDM0ZFhHTVpZR054SmtCLF9Nc0V5NkI2TjZ6MkZPWUxsUndtMXlyZzA5YzdDdjVU0100gEzqe9Je",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": true,
      "requirements": {
        "missing": [
          "biometric_consent",
          "face",
          "id_document_front",
          "id_document_back"
        ]
      },
      "selfie": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.5,
        "high_res_image_max_dimension": 1440,
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 800,
        "max_centered_threshold_x": 0.2,
        "max_centered_threshold_y": 0.2,
        "max_coverage_threshold": 0.8,
        "min_coverage_threshold": 0.07,
        "min_edge_threshold": 0.05,
        "models": {
          "face_detector_min_iou": 0.8,
          "face_detector_min_score": 0.8,
          "face_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/a8bcf0129dcd29084f6797ede7e0be86f9e11ed5/assets/face_detectors/tflite/2022-05-23/model.tflite"
        },
        "num_samples": 8,
        "sample_interval": 250,
        "training_consent_text": "Allow Stripe to use your images to improve our biometric verification technology. You can remove Stripe's permissions at any time by <a href='mailto:privacy@stripe.com'>contacting Stripe</a>. <a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a>"
      },
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Andrew's Audio will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Andrew's Audio will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
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

internal val FILE_UPLOAD_SUCCESS_JSON_STRING = """
    {
      "id": "file_1KZUtnEAjaOkiuGM9AuSSXXO",
      "object": "file",
      "created": 1646376359,
      "expires_at": null,
      "filename": "initialScreen.png",
      "purpose": "identity_private",
      "size": 136000,
      "title": null,
      "type": "png",
      "url": null
    }
""".trimIndent()

internal val VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ID_NUMBER_JSON_STRING = """
    {
      "id": "vs_1MOrPwEGkPhabJTjzCzKF4DM",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": true
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xTEliaExFR2tQaGFiSlRqLF9OOTlqVW84aWdKakk3dlBuM3gwWUdiejVrTkdUSWht0100PuDMlvY6",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "biometric_consent",
          "id_document_front",
          "id_document_back",
          "id_number"
        ]
      },
      "selfie": null,
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Tora's catfood will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Tora's catfood will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
    }

""".trimIndent()

internal val VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ADDRESS_JSON_STRING = """
    {
      "id": "vs_1MRjwTEGkPhabJTjIEKiUmmS",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": true
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/live_YWNjdF8xTEliaExFR2tQaGFiSlRqLF9OQzhDRjQ5TXI5ZGdyM0tKWDI1RzBKMGNTQ3JyNGVU0100vUQ0sLfw",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": true,
      "requirements": {
        "missing": [
          "address",
          "biometric_consent",
          "id_document_front",
          "id_document_back"
        ]
      },
      "selfie": null,
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Tora's catfood will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Tora's catfood will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ADDRESS_AND_ID_NUMBER_JSON_STRING = """
    {
      "id": "vs_1MTb71EGkPhabJTjK2kJQ5xI",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": true
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xTEliaExFR2tQaGFiSlRqLF9ORTNESmF3b2JCVldkRUladDZTMEw3TFk3aUt5N1BE0100D2Sn0qSX",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "address",
          "biometric_consent",
          "id_document_front",
          "id_document_back",
          "id_number"
        ]
      },
      "selfie": null,
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Tora's catfood will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Tora's catfood will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_TYPE_ID_NUMBER_JSON_STRING = """
    {
      "id": "vs_1MOrMgEGkPhabJTjkbUNVfh6",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": false
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xTEliaExFR2tQaGFiSlRqLF9OOTlnUXJWaWJSMGg3U2NzTGlQODRHR1BhbzlTd1BT0100mJdJgeY7",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
    "body": null,
    "get_started_button_text": "Get started",
    "lines": [
      {
        "content": "You'll provide personal information including your name and phone number.",
        "icon": "document"
      },
      {
        "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
        "icon": "dispute_protection"
      },
      {
        "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
        "icon": "lock"
      }
    ],
    "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
    "time_estimate": null,
    "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "dob",
          "id_number",
          "name"
        ]
      },
      "selfie": null,
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Tora's catfood will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Tora's catfood will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
    }
""".trimIndent()

internal val VERIFICATION_PAGE_TYPE_ADDRESS_JSON_STRING = """
    {
      "id": "vs_1MOrKBEGkPhabJTjBA2ohFAW",
      "object": "identity.verification_page",
      "biometric_consent": {
        "accept_button_text": "Agree and continue",
        "body": null,
        "decline_button_text": "Decline",
        "lines": [
          {
            "content": "You'll scan a valid <a href='stripe_bottomsheet://open/consent_photo_id'>photo ID</a>.",
            "icon": "camera"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "scroll_to_continue_button_text": "Scroll to continue",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "country_not_listed": {
        "address_from_other_country_text_button_text": "Have an Address from another country?",
        "body": "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity.",
        "cancel_button_text": "Cancel verification",
        "id_from_other_country_text_button_text": "Have an ID from another country?",
        "title": "We cannot verify your identity"
      },
      "document_capture": {
        "autocapture_timeout": 8000,
        "file_purpose": "identity_private",
        "high_res_image_compression_quality": 0.92,
        "high_res_image_crop_padding": 0.08,
        "high_res_image_max_dimension": 3000,
        "ios_id_card_back_barcode_timeout": 3000,
        "ios_id_card_back_country_barcode_symbologies": {
          "CA": "pdf417",
          "US": "pdf417"
        },
        "low_res_image_compression_quality": 0.82,
        "low_res_image_max_dimension": 3000,
        "models": {
          "id_detector_min_iou": 0.8,
          "id_detector_min_score": 0.5,
          "id_detector_url": "https://b.stripecdn.com/gelato-statics-srv/assets/d137be6ecc86477800ea4ef82154174092dc4c16/assets/id_detectors/tflite/2022-08-19/model.tflite"
        },
        "motion_blur_min_duration": 500,
        "motion_blur_min_iou": 0.95,
        "require_live_capture": false
      },
      "document_select": {
        "body": null,
        "button_text": "Next",
        "id_document_type_allowlist": {
          "driving_license": "Driver's license",
          "id_card": "Identity card",
          "passport": "Passport"
        },
        "title": "Which form of identification do you want to use?"
      },
      "fallback_url": "https://verify.stripe.com/start/test_YWNjdF8xTEliaExFR2tQaGFiSlRqLF9OOTlkVWhpa0hIa2s2RndYTjFxMmNwSGdxYm9hbno40100QbvpMKxr",
      "individual": {
        "address_countries": {
          "AT": "Austria",
          "AU": "Australia",
          "BE": "Belgium",
          "BR": "Brazil",
          "CA": "Canada",
          "CH": "Switzerland",
          "CZ": "Czech Republic",
          "DE": "Germany",
          "DK": "Denmark",
          "ES": "Spain",
          "FI": "Finland",
          "FR": "France",
          "GB": "United Kingdom",
          "HK": "Hong Kong",
          "ID": "Indonesia",
          "IE": "Ireland",
          "IT": "Italy",
          "LU": "Luxembourg",
          "MT": "Malta",
          "MX": "Mexico",
          "MY": "Malaysia",
          "NL": "Netherlands",
          "NO": "Norway",
          "PL": "Poland",
          "PT": "Portugal",
          "RO": "Romania",
          "SE": "Sweden",
          "SG": "Singapore",
          "SK": "Slovakia",
          "TH": "Thailand",
          "US": "United States"
        },
        "address_country_not_listed_text_button_text": "My country is not listed",
        "button_text": "Submit",
        "id_number_countries": {
          "BR": "Brazil",
          "SG": "Singapore",
          "US": "United States"
        },
        "id_number_country_not_listed_text_button_text": "My country is not listed",
        "title": "Provide personal information",
        "phone_number_countries": {
          "US": "United States"
        }
      },
      "individual_welcome": {
        "body": null,
        "get_started_button_text": "Get started",
        "lines": [
          {
            "content": "You'll provide personal information including your name and phone number.",
            "icon": "document"
          },
          {
            "content": "The information you provide Stripe will help us <a href='stripe_bottomsheet://open/consent_identity'>confirm your identity</a>.",
            "icon": "dispute_protection"
          },
          {
            "content": "Andrew's Audio will only have access to this <a href='stripe_bottomsheet://open/consent_verification_data'>verification data</a>.",
            "icon": "lock"
          }
        ],
        "privacy_policy": "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • <a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>",
        "time_estimate": null,
        "title": "Andrew's Audio works with Stripe to verify your identity"
      },
      "livemode": false,
      "requirements": {
        "missing": [
          "address",
          "dob",
          "name"
        ]
      },
      "selfie": null,
      "status": "requires_input",
      "submitted": false,
      "closed": false,
      "success": {
        "body": "<p>Thank you for providing your information. Tora's catfood will reach out if additional details are required.</p><p><b>Next steps</b></p><p>Tora's catfood will contact you regarding the outcome of your identification process.</p><p><b>More about Stripe Identity</b></p><p><a href='https://support.stripe.com/questions/common-questions-about-stripe-identity'>Common questions about Stripe Identity</a></p><p><a href='https://stripe.com/privacy-center/legal#stripe-identity'>Learn how Stripe uses data</a></p><p><a href='https://stripe.com/privacy'>Stripe Privacy Policy</a></p><p><a href='mailto:privacy@stripe.com'>Contact Stripe</a></p>",
        "button_text": "Complete",
        "title": "Verification submitted"
      },
      "unsupported_client": false
    }
""".trimIndent()

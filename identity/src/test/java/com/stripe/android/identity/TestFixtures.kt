package com.stripe.android.identity

import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import com.stripe.android.identity.networking.VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE_JSON_STRING
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import kotlinx.serialization.json.Json

internal const val ERROR_BODY = "errorBody"
internal const val ERROR_BUTTON_TEXT = "error button text"
internal const val ERROR_TITLE = "errorTitle"

internal val CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = false
)

internal val CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = true
)

internal val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

internal val SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE: VerificationPage =
    json.decodeFromString(
        VerificationPage.serializer(),
        VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
    )

internal val SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE: VerificationPage =
    json.decodeFromString(
        VerificationPage.serializer(),
        VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE_JSON_STRING
    )

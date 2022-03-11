package com.stripe.android.identity

import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements

internal const val ERROR_BODY = "errorBody"
internal const val ERROR_BUTTON_TEXT = "error button text"
internal const val ERROR_TITLE = "errorTitle"

internal val CORRECT_VERIFICATION_PAGE_DATA = VerificationPageData(
    id = "id",
    objectType = "type",
    requirements = VerificationPageDataRequirements(
        errors = emptyList(),
        missing = emptyList()
    ),
    status = VerificationPageData.Status.VERIFIED,
    submitted = false
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

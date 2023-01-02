package com.stripe.android.identity.utils

import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_DRIVER_LICENSE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_PASSPORT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_PASSPORT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.networking.models.Requirement

internal fun Int.fragmentIdToRequirement(): List<Requirement> = when (this) {
    R.id.consentFragment -> {
        listOf(Requirement.BIOMETRICCONSENT)
    }
    R.id.docSelectionFragment -> {
        listOf(Requirement.IDDOCUMENTTYPE)
    }
    R.id.IDUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.passportUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.driverLicenseUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.IDScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.passportScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.driverLicenseScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.selfieFragment -> {
        listOf(Requirement.FACE)
    }
    else -> {
        listOf()
    }
}

internal fun Int.fragmentIdToScreenName(): String = when (this) {
    R.id.consentFragment -> {
        SCREEN_NAME_CONSENT
    }
    R.id.docSelectionFragment -> {
        SCREEN_NAME_DOC_SELECT
    }
    R.id.IDScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_ID
    }
    R.id.passportScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_PASSPORT
    }
    R.id.driverLicenseScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
    }
    R.id.IDUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_ID
    }
    R.id.passportUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_PASSPORT
    }
    R.id.driverLicenseUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_DRIVER_LICENSE
    }
    R.id.selfieFragment -> {
        SCREEN_NAME_SELFIE
    }
    R.id.confirmationFragment -> {
        SCREEN_NAME_CONFIRMATION
    }
    R.id.cameraPermissionDeniedFragment -> {
        SCREEN_NAME_ERROR
    }
    R.id.errorFragment -> {
        SCREEN_NAME_ERROR
    }
    R.id.couldNotCaptureFragment -> {
        SCREEN_NAME_ERROR
    }
    else -> {
        throw IllegalArgumentException("Invalid fragment ID: $this")
    }
}

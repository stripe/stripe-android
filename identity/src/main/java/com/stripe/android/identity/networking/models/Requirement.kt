package com.stripe.android.identity.networking.models

import androidx.annotation.IdRes
import com.stripe.android.identity.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class Requirement {
    @SerialName("biometric_consent")
    BIOMETRICCONSENT,

    @SerialName("id_document_back")
    IDDOCUMENTBACK,

    @SerialName("id_document_front")
    IDDOCUMENTFRONT,

    @SerialName("id_document_type")
    IDDOCUMENTTYPE,

    @SerialName("face")
    FACE;

    internal companion object {
        private val SCAN_UPLOAD_FRAGMENT_ID_SET = setOf(
            R.id.driverLicenseUploadFragment,
            R.id.IDUploadFragment,
            R.id.passportUploadFragment,
            R.id.driverLicenseScanFragment,
            R.id.IDScanFragment,
            R.id.passportScanFragment
        )

        /**
         * Checks whether the Requirement matches the Fragment the error occurred from.
         */
        fun Requirement.matchesFromFragment(@IdRes fromFragment: Int) =
            when (this) {
                BIOMETRICCONSENT -> {
                    fromFragment == R.id.consentFragment
                }
                IDDOCUMENTBACK -> {
                    SCAN_UPLOAD_FRAGMENT_ID_SET.contains(fromFragment)
                }
                IDDOCUMENTFRONT -> {
                    SCAN_UPLOAD_FRAGMENT_ID_SET.contains(fromFragment)
                }
                IDDOCUMENTTYPE -> {
                    fromFragment == R.id.docSelectionFragment
                }
                FACE -> {
                    fromFragment == R.id.selfieFragment
                }
            }
    }
}

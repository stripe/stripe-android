package com.stripe.android.identity.networking.models

import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DocSelectionDestination
import com.stripe.android.identity.navigation.DriverLicenseScanDestination
import com.stripe.android.identity.navigation.DriverLicenseUploadDestination
import com.stripe.android.identity.navigation.IDScanDestination
import com.stripe.android.identity.navigation.IDUploadDestination
import com.stripe.android.identity.navigation.PassportScanDestination
import com.stripe.android.identity.navigation.PassportUploadDestination
import com.stripe.android.identity.navigation.SelfieDestination
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
    FACE,

    @SerialName("id_number")
    IDNUMBER,

    @SerialName("dob")
    DOB,

    @SerialName("name")
    NAME,

    @SerialName("address")
    ADDRESS;

    internal companion object {
        private val SCAN_UPLOAD_ROUTE_SET = setOf(
            DriverLicenseUploadDestination.ROUTE,
            IDUploadDestination.ROUTE,
            PassportUploadDestination.ROUTE,
            DriverLicenseScanDestination.ROUTE,
            IDScanDestination.ROUTE,
            PassportScanDestination.ROUTE
        )

        /**
         * Checks whether the Requirement matches the route the error occurred from.
         */
        fun Requirement.matchesFromRoute(fromRoute: String) =
            when (this) {
                BIOMETRICCONSENT -> {
                    fromRoute == ConsentDestination.ROUTE.route
                }
                IDDOCUMENTBACK -> {
                    SCAN_UPLOAD_ROUTE_SET.any {
                        it.route == fromRoute
                    }
                }
                IDDOCUMENTFRONT -> {
                    SCAN_UPLOAD_ROUTE_SET.any {
                        it.route == fromRoute
                    }
                }
                IDDOCUMENTTYPE -> {
                    fromRoute == DocSelectionDestination.ROUTE.route
                }
                FACE -> {
                    fromRoute == SelfieDestination.ROUTE.route
                }
                else -> {
                    false
                }
            }
    }
}

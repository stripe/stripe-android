package com.stripe.android.identity.networking.models

import android.content.Context
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DocSelectionDestination
import com.stripe.android.identity.navigation.DriverLicenseScanDestination
import com.stripe.android.identity.navigation.DriverLicenseUploadDestination
import com.stripe.android.identity.navigation.IDScanDestination
import com.stripe.android.identity.navigation.IDUploadDestination
import com.stripe.android.identity.navigation.IdentityTopLevelDestination
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.IndividualWelcomeDestination
import com.stripe.android.identity.navigation.PassportScanDestination
import com.stripe.android.identity.navigation.PassportUploadDestination
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.finalErrorDestination
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
    ADDRESS,

    @SerialName("phone")
    PHONE,

    @SerialName("phone_otp")
    PHONE_OTP;

    internal companion object {
        private val SCAN_UPLOAD_ROUTE_SET = setOf(
            DriverLicenseUploadDestination.ROUTE,
            IDUploadDestination.ROUTE,
            PassportUploadDestination.ROUTE,
            DriverLicenseScanDestination.ROUTE,
            IDScanDestination.ROUTE,
            PassportScanDestination.ROUTE
        )

        val INDIVIDUAL_REQUIREMENT_SET = setOf(
            NAME,
            DOB,
            ADDRESS,
            IDNUMBER
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
                DOB, NAME, IDNUMBER, ADDRESS, PHONE, PHONE_OTP -> {
                    fromRoute == IndividualDestination.ROUTE.route
                }
            }

        /**
         * Infers the next destination based on a list of Requirements.
         */
        fun List<Requirement>?.nextDestination(context: Context): IdentityTopLevelDestination =
            when {
                this == null -> {
                    context.finalErrorDestination()
                }
                // BIOMETRICCONSENT is present when type is DOCUMENT
                contains(BIOMETRICCONSENT) -> {
                    ConsentDestination
                }
                intersect(listOf(IDDOCUMENTTYPE, IDDOCUMENTFRONT, IDDOCUMENTBACK)).isNotEmpty() -> {
                    DocSelectionDestination
                }

                contains(FACE) -> {
                    SelfieDestination
                }
                // NAME and DOB is present when type is not DOCUMENT
                intersect(listOf(NAME, DOB)).isNotEmpty() -> {
                    IndividualWelcomeDestination
                }
                // If NAME and ODB is not present but IDNUMBER or ADDRESS is present,
                // then type is DOCUMENT, user has already uploaded document and selfie.
                intersect(listOf(IDNUMBER, ADDRESS)).isNotEmpty() -> {
                    IndividualDestination
                }
                isEmpty() -> {
                    ConfirmationDestination
                }
                else -> {
                    context.finalErrorDestination()
                }
            }
    }
}

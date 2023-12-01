package com.stripe.android.identity.networking.models

import android.content.Context
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DocWarmupDestination
import com.stripe.android.identity.navigation.DocumentScanDestination
import com.stripe.android.identity.navigation.DocumentUploadDestination
import com.stripe.android.identity.navigation.IdentityTopLevelDestination
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.IndividualWelcomeDestination
import com.stripe.android.identity.navigation.OTPDestination
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

    @SerialName("phone_number")
    PHONE_NUMBER,

    @SerialName("phone_otp")
    PHONE_OTP;

    internal companion object {

        val INDIVIDUAL_REQUIREMENT_SET = setOf(
            NAME,
            DOB,
            ADDRESS,
            IDNUMBER
        )

        private val REQUIREMENTS_SUPPORTS_FORCE_CONFIRM = setOf(
            IDDOCUMENTFRONT,
            IDDOCUMENTBACK
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
                    fromRoute == DocumentScanDestination.ROUTE.route ||
                        fromRoute == DocumentUploadDestination.ROUTE.route
                }

                IDDOCUMENTFRONT -> {
                    fromRoute == DocumentScanDestination.ROUTE.route ||
                        fromRoute == DocumentUploadDestination.ROUTE.route
                }

                FACE -> {
                    fromRoute == SelfieDestination.ROUTE.route
                }

                DOB, NAME, IDNUMBER, ADDRESS, PHONE_NUMBER -> {
                    fromRoute == IndividualDestination.ROUTE.route
                }

                PHONE_OTP -> {
                    fromRoute == OTPDestination.ROUTE.route
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

                intersect(listOf(IDDOCUMENTFRONT, IDDOCUMENTBACK)).isNotEmpty() -> {
                    DocWarmupDestination
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

        fun Requirement.supportsForceConfirm() =
            REQUIREMENTS_SUPPORTS_FORCE_CONFIRM.contains(this)
    }
}

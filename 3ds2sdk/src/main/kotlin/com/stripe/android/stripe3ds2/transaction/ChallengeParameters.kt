package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// NOTE: Copied from reference app spec

/**
 * The ChallengeParameters class shall hold the parameters that are required to conduct the
 * challenge process.
 * Note: It is mandatory to set values for these parameters.
 */
@Parcelize
data class ChallengeParameters(

    /**
     * The 3DS Server Transaction ID. This ID is a transaction identifier assigned by the
     * 3DS Server to uniquely identify a single transaction.
     */
    var threeDsServerTransactionId: String? = null,

    /**
     * Transaction ID assigned by the ACS to uniquely identify a single transaction.
     */
    var acsTransactionId: String? = null,

    /**
     * EMVCo assigns the ACS this identifier after running the EMV 3-D Secure Testing and Approvals
     * process on the ACS.
     */
    var acsRefNumber: String? = null,

    /**
     * The ACS signed content. This content includes the
     * ACS URL, ACS ephemeral public key, and SDK ephemeral public key.
     */
    var acsSignedContent: String? = null
) : Parcelable

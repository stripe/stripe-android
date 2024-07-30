package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// NOTE: Copied from reference app spec

/**
 * The AuthenticationRequestParameters class shall hold transaction data that the App
 * passes to the 3DS Server for creating the AReq.
 */
@Parcelize
data class AuthenticationRequestParameters(
    /**
     * The encrypted device data as a JWE string.
     */
    val deviceData: String,

    /**
     * The SDK Transaction ID. When called, the 3DS SDK uses a secure random function to generate
     * a Transaction ID in UUID format.
     */
    val sdkTransactionId: SdkTransactionId,

    /**
     * The SDK App ID. The 3DS SDK uses a secure random function to generate the App ID in UUID
     * format. This ID is unique and is generated during installation and update of the app on the
     * Cardholder’s device.
     */
    val sdkAppId: String,

    /**
     * The SDK Reference Number.
     */
    val sdkReferenceNumber: String,

    /**
     * The SDK Ephemeral Public Key. An ephemeral key pair is used to establish a secure session
     * between the 3DS SDK and the ACS.
     *
     * During each transaction, the createTransaction method generates a fresh ephemeral key
     * pair and the [sdkEphemeralPublicKey] returns the public key component of the
     * same as a String representation of a JWK object.
     */
    val sdkEphemeralPublicKey: String,

    /**
     * The protocol version that is used for the transaction.
     * The SDK receives the protocol version as a parameter in the createTransaction method
     * and determines whether it supports the version.
     * If the SDK does not receive the protocol version as a parameter in the createTransaction
     * method, then it returns the latest version that it supports.
     */
    val messageVersion: String
) : Parcelable

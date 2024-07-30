package com.stripe.android.stripe3ds2.transaction

/**
 * An object that implements the Transaction interface shall hold parameters that the 3DS Server
 * requires to create AReq messages and to perform the Challenge Flow.
 */
interface Transaction {
    /**
     * When the 3DS Requestor App calls the getAuthenticationRequestParameters method,
     * the 3DS SDK shall encrypt the device information that it collects during initialization and
     * send this information along with the SDK information to the 3DS Requestor App.
     * The app includes this information in its message to the 3DS Server.
     *
     *
     * The 3DS SDK encrypts the device information by using the DS public key.
     * This key is identified based on the directoryServerID that is passed to the createTransaction
     * method.
     * The 3DS SDK can use A128CBC-HS256 or A128GCM as the encryption algorithm.
     * For more information about 3DS SDK encryption, refer to Section 6.2.2, "Function I: 3DS SDK
     * Encryption to DS" in the EMV 3DS Protocol Specification.
     *
     *
     * The 3DS SDK shall generate an ephemeral key pair that is required for subsequent
     * communication with the ACS if a challenge must be applied.
     * For more information, refer to 3DS SDK – ACS Secure Channel.
     *
     *
     * The getAuthenticationRequestParameters method shall be called for every transaction.
     *
     * @return This method returns an AuthenticationRequestParameters object that contains device
     * information and 3DS SDK information.
     */
    suspend fun createAuthenticationRequestParameters(): AuthenticationRequestParameters

    /**
     * The [SdkTransactionId] for this 3DS2 transaction.
     */
    val sdkTransactionId: SdkTransactionId

    /**
     * If the ARes that is returned indicates that the Challenge Flow must be applied, the 3DS
     * Requestor App calls the [createInitChallengeArgs] method with the required input parameters.
     * The [InitChallengeArgs] will be used to initiate the challenge process.
     *
     * Note: The [createInitChallengeArgs] method shall be called only when the Challenge Flow is to
     * be applied.
     *
     * When the [createInitChallengeArgs] method is called and the Intent is started, control of the
     * app is passed to the 3DS SDK. At this point:
     *  * The 3DS SDK shall start a time counter to measure the overall time taken by the
     * challenge process.
     *  * The 3DS SDK shall check if the CA public key (root) of the Directory Server CA (DS-CA)
     * is present, based on the directoryServerID that was passed to the createTransaction method.
     *  * The 3DS SDK shall use the CA public key of the DS-CA to validate the ACS signed content
     * JWS object. Based on the information included in the JWS object, the algorithm used to
     * perform the validation can be PS256 or ES256.
     *  * The 3DS SDK shall complete the Diffie-Hellman key exchange process according to JWA (RFC
     * 7518) in Direct Key Agreement mode using curve P-256. The output of this process is a pair of
     * CEKs.
     *  * The 3DS SDK shall use the CEKs to encrypt the CReq messages and decrypt the CRes
     * messages.
     *
     * For more information about the algorithms used for validation, and the CEKs, refer to the
     * "3DS SDK Secure Channel Set-Up" section in Section 6.2.3, "Function J: 3DS SDK—ACS Secure
     * Channel Set-Up" in the EMV 3DS Protocol Specification.
     * The 3DS SDK shall display the challenge to the Cardholder. The following steps shall take
     * place during the challenge process:
     *
     *  * The 3DS Requestor App’s current screen shall be closed either before the challenge
     * screen is launched or before the ChallengeStatusReceiver callback is invoked by the 3DS SDK.
     * This is to prevent the Cardholder from revisiting the card details screen using the Back
     * button during the challenge process. For more information about the user experience when the
     * Cardholder taps the Back button, refer to Figure 4-1.
     *  * The 3DS SDK shall exchange two or more CReq and CRes messages with the ACS.
     *  * The 3DS SDK shall send the challenge status back to the 3DS Requestor App by using the
     * ChallengeStatusReceiver callback functions.
     *  * The 3DS SDK shall clean up resources that are held by the Transaction object.
     *
     * At any point of time, if the time taken by the challenge process (as measured by the time
     * counter) exceeds the timeout value passed by the 3DS Requestor App, then the 3DS SDK shall
     * call the timedout method of the ChallengeStatusReceiver callback object and clean up
     * resources that are held by the Transaction object.
     *
     * @param challengeParameters ACS details (contained in the ARes) required by the 3DS SDK to
     * conduct the challenge process during the transaction.
     * The following details are mandatory:
     *  * 3DS Server Transaction ID
     *  * ACS Transaction ID
     *  * ACS Reference Number
     *  * ACS Signed Content
     *
     * @param timeoutMins Timeout interval (in minutes) within which the challenge
     * process must be completed. The minimum timeout interval shall
     * be 5 minutes.
     *
     * @param intentData Stripe Intent data that will be included in the Activity result
     */
    fun createInitChallengeArgs(
        challengeParameters: ChallengeParameters,
        timeoutMins: Int,
        intentData: IntentData
    ): InitChallengeArgs
}

package com.stripe.android.stripe3ds2.service

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.init.Warning
import com.stripe.android.stripe3ds2.transaction.Transaction

// NOTE: Copied from reference app spec

/**
 * The ThreeDS2Service interface is the main 3DS SDK interface. It shall provide
 * methods to process transactions.
 */
interface ThreeDS2Service {

    /**
     * @return the current 3DS2 SDK version
     */
    val sdkVersion: String

    /**
     * @return list of [Warning] detected by the 3DS2 SDK during initialization
     */
    val warnings: List<Warning>

    /**
     * The createTransaction method creates an instance of Transaction through which the app gets
     * the data that is required to perform the transaction. The app calls the createTransaction
     * method for each transaction that is to be processed. When the createTransaction
     * method is called:
     *  - The 3DS SDK uses the information adhering to the protocol version
     * passed in the optional messageVersion parameter, if it supports the
     * protocol version. If it does not support the protocol version, it
     * generates an InvalidInputException. If the messageVersion parameter is
     * empty or null, the highest protocol version that the 3DS SDK supports is
     * used. If Challenge Flow is triggered for the transaction, the 3DS SDK
     * uses the same protocol version during the challenge process.
     *  - The 3DS SDK uses a secure random function to generate a Transaction
     * ID in UUID format. This ID is used to uniquely identify each transaction.
     *  - The 3DS SDK generates a fresh ephemeral key pair. This key pair is
     * used to establish a secure session between the 3DS SDK and the ACS
     * subsequently during the transaction.
     *
     * @param directoryServerID Registered Application Provider Identifier (RID)
     * that is unique to the Payment System.<br></br>
     * RIDs are defined by the ISO 7816-5 standard.<br></br>
     * RIDs are issued by the ISO/IEC 7816- 5 registration authority.<br></br>
     * Contains a 5-byte value.<br></br>
     * The 3DS SDK encrypts the device information by using the DS public key.
     * This key is identified based on the directoryServerID that is passed to
     * the createTransaction method.<br></br>
     * Note: The 3DS SDK shall have the DS Public Keys of all the 3-D Secure
     * participating Directory Servers.
     * @param messageVersion (optional) Protocol version according to which the
     * transaction shall be created.
     * @return This method returns an instance of the Transaction interface.
     * @throws InvalidInputException This exception shall be thrown if an input
     * parameter is invalid. This also includes an invalid Directory Server ID
     * or a protocol version that the 3DS SDK does not support.
     * @throws SDKRuntimeException This exception shall be thrown if an internal
     * error is encountered by the 3DS SDK.
     */
    @Throws(
        InvalidInputException::class, SDKRuntimeException::class
    )
    fun createTransaction(
        directoryServerID: String,
        messageVersion: String?
    ): Transaction
}

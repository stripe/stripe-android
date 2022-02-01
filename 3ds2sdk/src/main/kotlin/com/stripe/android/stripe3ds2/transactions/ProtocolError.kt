package com.stripe.android.stripe3ds2.transactions

internal enum class ProtocolError constructor(
    val code: Int,
    val description: String
) {
    InvalidMessageReceived(
        101,
        "Message is not AReq, ARes, CReq, CRes, PReq, PRes, RReq, or RRes"
    ),

    UnsupportedMessageVersion(
        102,
        "Message Version Number received is not valid for the receiving component."
    ),

    RequiredDataElementMissing(
        201,
        "A message element required as defined in Table A.1 is missing from the message."
    ),

    UnrecognizedCriticalMessageExtensions(
        202,
        "Critical message extension not recognised."
    ),

    InvalidDataElementFormat(
        203,
        "Data element not in the required format or value is invalid as defined in Table A.1"
    ),

    InvalidTransactionId(
        301,
        "Transaction ID received is not valid for the receiving component."
    ),

    DataDecryptionFailure(
        302,
        "Data could not be decrypted by the receiving system due to technical or other reason."
    ),

    TransactionTimedout(
        402,
        "Transaction timed-out."
    )
}

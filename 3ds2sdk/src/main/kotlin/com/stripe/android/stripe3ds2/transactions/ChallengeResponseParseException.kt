package com.stripe.android.stripe3ds2.transactions

internal class ChallengeResponseParseException internal constructor(
    val code: Int,
    val description: String,
    val detail: String
) : Exception("$code - $description ($detail)") {

    constructor(
        protocolError: ProtocolError,
        detail: String
    ) : this(protocolError.code, protocolError.description, detail)

    internal companion object {
        @JvmStatic
        fun createRequiredDataElementMissing(fieldName: String): ChallengeResponseParseException {
            return ChallengeResponseParseException(
                ProtocolError.RequiredDataElementMissing.code,
                "A message element required as defined in Table A.1 is missing from the message.",
                fieldName
            )
        }

        @JvmStatic
        fun createInvalidDataElementFormat(fieldName: String): ChallengeResponseParseException {
            return ChallengeResponseParseException(
                ProtocolError.InvalidDataElementFormat.code,
                "Data element not in the required format or value is invalid as defined in Table A.1",
                fieldName
            )
        }
    }
}

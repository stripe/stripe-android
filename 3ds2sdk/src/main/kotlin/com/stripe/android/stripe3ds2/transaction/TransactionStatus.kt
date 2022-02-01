package com.stripe.android.stripe3ds2.transaction

/**
 * Transaction Status is populated in the transStatus field of the final CRes response.
 *
 *
 *  * Y - Authentication Verification Successful.
 *  * N - Not Authenticated /Account Not Verified; Transaction denied.
 *  * U - Authentication/Account Verification Could Not Be Performed;
 * Technical or other problem, as indicated in ARes or RReq.
 *  * A - Attempts Processing Performed; Not Authenticated/Verified, but a proof of attempted
 * authentication/verification is provided.
 *  * C - Challenge Required; Additional authentication is required using the CReq/CRes.
 *  * D - Challenge Required; Decoupled Authentication confirmed.
 *  * R - Authentication/Account Verification Rejected; Issuer is rejecting
 * authentication/verification and request that authorisation not be attempted.
 *  * I - Informational Only; 3DS Requestor challenge preference acknowledged.
 *
 */
enum class TransactionStatus constructor(val code: String) {
    VerificationSuccessful("Y"),
    VerificationDenied("N"),
    VerificationNotPerformed("U"),
    VerificationAttempted("A"),
    ChallengeAdditionalAuth("C"),
    ChallengeDecoupledAuth("D"),
    VerificationRejected("R"),
    InformationOnly("I");

    companion object {
        fun fromCode(code: String?): TransactionStatus? {
            return if (code == null) {
                null
            } else {
                values().firstOrNull { it.code == code }
            }
        }
    }
}

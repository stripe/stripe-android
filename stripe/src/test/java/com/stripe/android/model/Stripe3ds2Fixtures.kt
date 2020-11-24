package com.stripe.android.model

import com.stripe.android.stripe3ds2.transaction.AuthenticationRequestParameters
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import java.util.UUID

internal object Stripe3ds2Fixtures {
    const val MESSAGE_VERSION = "2.1.0"

    private const val DEVICE_DATA = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21cSPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5TqsxMn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35UI1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46NDoi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoRq218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9pDjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oRagx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluOpn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p01pl8CKukuXzjLeY3a_Ew"
    private val SDK_TRANSACTION_ID = UUID.randomUUID().toString()
    private const val SDK_APP_ID = "1.0.0"
    private const val SDK_REFERENCE_NUMBER = "3DS_LOA_SDK_STIN_12345"
    private const val SDK_EPHEMERAL_PUBLIC_KEY = "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"b23da28b-d611-46a8-93af-44ad57ce9c9d\",\"x\":\"hSwyaaAp3ppSGkpt7d9G8wnp3aIXelsZVo05EPpqetg\",\"y\":\"OUVOv9xPh5RYWapla0oz3vCJWRRXlDmppy5BGNeSl-A\"}"

    fun createAreqParams(
        sdkTransactionId: SdkTransactionId
    ): AuthenticationRequestParameters {
        return AuthenticationRequestParameters(
            deviceData = DEVICE_DATA,
            messageVersion = MESSAGE_VERSION,
            sdkAppId = SDK_APP_ID,
            sdkEphemeralPublicKey = SDK_EPHEMERAL_PUBLIC_KEY,
            sdkReferenceNumber = SDK_REFERENCE_NUMBER,
            sdkTransactionId = sdkTransactionId
        )
    }
}

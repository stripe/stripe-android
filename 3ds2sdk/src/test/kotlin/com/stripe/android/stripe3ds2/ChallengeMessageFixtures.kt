package com.stripe.android.stripe3ds2

import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType
import java.util.UUID

object ChallengeMessageFixtures {
    const val MESSAGE_VERSION_210 = "2.1.0"

    val ACS_TRANS_ID = UUID.randomUUID().toString()
    val SDK_TRANS_ID = SdkTransactionId.create()
    val SERVER_TRANS_ID = UUID.randomUUID().toString()
    val THREE_DS_APP_URL = UUID.randomUUID().toString()

    val ISSUER_IMAGE = ChallengeResponseData.Image(
        "http://mediumUrl",
        "http://highUrl",
        "http://extraHighUrl"
    )

    val PAYMENT_SYSTEM_IMAGE = ChallengeResponseData.Image(
        "http://paymediumUrl",
        "http://payhighUrl",
        "http://payextraHighUrl"
    )

    val CREQ = ChallengeRequestData(
        acsTransId = ACS_TRANS_ID,
        threeDsServerTransId = SERVER_TRANS_ID,
        sdkTransId = SDK_TRANS_ID,
        messageVersion = MESSAGE_VERSION_210,
        threeDSRequestorAppURL = THREE_DS_APP_URL
    )

    val CRES = ChallengeResponseData(
        sdkTransId = SDK_TRANS_ID,
        serverTransId = SERVER_TRANS_ID,
        acsTransId = ACS_TRANS_ID,
        messageVersion = MESSAGE_VERSION_210
    )

    internal val CRES_TEXT_DATA = CRES.copy(
        uiType = UiType.Text,
        shouldShowChallengeInfoTextIndicator = true,
        issuerImage = ISSUER_IMAGE,
        paymentSystemImage = PAYMENT_SYSTEM_IMAGE
    )
}

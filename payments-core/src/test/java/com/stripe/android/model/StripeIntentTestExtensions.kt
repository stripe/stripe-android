package com.stripe.android.model

fun StripeIntent.NextActionData.SdkData.Use3DS2.copy(
    source: String = this.source,
    serverName: String = this.serverName,
    transactionId: String = this.transactionId,
    serverEncryption: StripeIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption = this.serverEncryption,
    threeDS2IntentId: String? = this.threeDS2IntentId,
    publishableKey: String? = this.publishableKey,
): StripeIntent.NextActionData.SdkData.Use3DS2 {
    return StripeIntent.NextActionData.SdkData.Use3DS2(
        source = source,
        serverName = serverName,
        transactionId = transactionId,
        serverEncryption = serverEncryption,
        threeDS2IntentId = threeDS2IntentId,
        publishableKey = publishableKey,
    )
}

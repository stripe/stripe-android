package com.stripe.android.stripe3ds2.init

import com.stripe.android.stripe3ds2.transaction.SdkTransactionId

fun interface DeviceDataFactory {
    suspend fun create(sdkReferenceNumber: String, sdkTransactionId: SdkTransactionId): Map<String, Any?>
}

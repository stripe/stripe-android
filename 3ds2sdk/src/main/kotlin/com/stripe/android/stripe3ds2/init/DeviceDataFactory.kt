package com.stripe.android.stripe3ds2.init

import androidx.annotation.RestrictTo
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface DeviceDataFactory {
    suspend fun create(sdkReferenceNumber: String, sdkTransactionId: SdkTransactionId): Map<String, Any?>
}

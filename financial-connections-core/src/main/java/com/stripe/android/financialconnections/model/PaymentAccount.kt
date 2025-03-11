package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.PaymentAccountSerializer
import kotlinx.serialization.Serializable

@Serializable(with = PaymentAccountSerializer::class)
sealed class PaymentAccount : Parcelable {
    abstract val id: String
}

package com.stripe.android.paymentsheet.model

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object MobileSessionParceler : Parceler<MobileSession.Session?> {
    override fun create(parcel: Parcel): MobileSession.Session? {
        return parcel.readString()?.let { MobileSession.decode(it) }
    }

    override fun MobileSession.Session?.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this?.let { Json.encodeToString(it) })
    }
}

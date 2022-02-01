package com.stripe.android.stripe3ds2.init

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.stripe.android.stripe3ds2.utils.Supplier

/**
 * Returns the device's hardware id.
 */
internal class HardwareIdSupplier(context: Context) : Supplier<HardwareId> {
    private val context: Context = context.applicationContext

    @SuppressLint("HardwareIds")
    override fun get(): HardwareId {
        val hardwareId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )
        return HardwareId(hardwareId.orEmpty())
    }
}

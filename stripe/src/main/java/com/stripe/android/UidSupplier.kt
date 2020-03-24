package com.stripe.android

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

internal class UidSupplier(context: Context) : Supplier<StripeUid> {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    private val deviceId: String?
        @SuppressLint("HardwareIds")
        get() {
            return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }

    @SuppressLint("HardwareIds")
    override fun get(): StripeUid {
        return StripeUid(deviceId.orEmpty())
    }
}

package com.stripe.android

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

internal class UidSupplier(context: Context) : Supplier<StripeUid> {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    @SuppressLint("HardwareIds")
    override fun get(): StripeUid {
        return StripeUid.create(
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        )
    }
}

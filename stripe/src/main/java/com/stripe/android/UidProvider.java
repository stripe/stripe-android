package com.stripe.android;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;

class UidProvider {
    @NonNull private final Context mContext;

    UidProvider(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @SuppressWarnings("HardwareIds")
    @NonNull
    String get() {
        return Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }
}

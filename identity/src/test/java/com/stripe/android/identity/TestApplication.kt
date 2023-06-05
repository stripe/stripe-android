package com.stripe.android.identity

import android.app.Application
import com.google.android.material.R as MaterialR

// A material themed application is needed to inflate MaterialToolbar in IdentityActivity
internal class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(MaterialR.style.Theme_MaterialComponents_DayNight_NoActionBar)
    }
}

package com.stripe.android.identity

import android.app.Application

// A material themed application is needed to inflate MaterialToolbar in IdentityActivity
internal class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_MaterialComponents_DayNight_NoActionBar)
    }
}

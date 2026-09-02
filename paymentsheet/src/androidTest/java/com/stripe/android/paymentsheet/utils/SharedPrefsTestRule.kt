package com.stripe.android.paymentsheet.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.link.account.DefaultLinkStore
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class SharedPrefsTestRule : TestWatcher() {
    override fun finished(description: Description?) {
        val context = ApplicationProvider.getApplicationContext<Context>()

        DefaultLinkStore(context).clear()
        PrefsTestStore(context).clear()

        super.finished(description)
    }
}

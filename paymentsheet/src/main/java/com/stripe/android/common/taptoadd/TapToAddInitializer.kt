package com.stripe.android.common.taptoadd

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.stripe.stripeterminal.TerminalApplicationDelegate
import com.stripe.stripeterminal.taptopay.TapToPay

internal class TapToAddInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val application = context.applicationContext as Application

        if (!TapToPay.isInTapToPayProcess()) {
            TerminalApplicationDelegate.onCreate(application)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(TapToAddInitializer::class.java)
    }
}

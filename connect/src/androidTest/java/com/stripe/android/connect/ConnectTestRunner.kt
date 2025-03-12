package com.stripe.android.connect

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.stripe.android.connect.di.DaggerTestStripeConnectComponent
import com.stripe.android.connect.di.StripeConnectComponent

@Suppress("unused")
class ConnectTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        StripeConnectComponent.replaceInstance(DaggerTestStripeConnectComponent.create())
        return super.newApplication(cl, className, context)
    }
}

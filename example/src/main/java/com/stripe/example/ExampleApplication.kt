package com.stripe.example

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.stripe.android.PaymentConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class ExampleApplication : Application() {

    override fun onCreate() {
//        PaymentConfiguration.init(this, Settings(this).publishableKey)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                delay(15.seconds)
                val url = URL("https://google.com")
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val bytes = connection.inputStream.readBytes()
                println("TOLUWANI => ${String(bytes)}")
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
            PaymentConfiguration.init(this@ExampleApplication, Settings(this@ExampleApplication).publishableKey)
        }

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .also {
                    if (IS_PENALTY_DEATH_ENABLED) {
                        it.penaltyDeath()
                    }
                }
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .penaltyDeath()
                .also {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        // this resulted in a crash when launching a transparent Activity in API 30
                        // > E InputDispatcher: channel ~ Channel is unrecoverably broken and will be disposed!
                        it.detectLeakedClosableObjects()
                    }
                }
                .build()
        )

        super.onCreate()
    }

    private companion object {
        private const val IS_PENALTY_DEATH_ENABLED = false
    }
}

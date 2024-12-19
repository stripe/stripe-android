package com.stripe.android.connect.example

import android.app.Application
import android.os.StrictMode
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.core.Logger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltAndroidApp
@OptIn(DelicateCoroutinesApi::class)
class App : Application() {

    @Inject lateinit var embeddedComponentService: EmbeddedComponentService

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    override fun onCreate() {
        super.onCreate()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )

        attemptLoadPublishableKey()
    }

    private fun attemptLoadPublishableKey() {
        GlobalScope.launch {
            try {
                embeddedComponentService.loadPublishableKey()
            } catch (e: FuelError) {
                logger.error("(App) Error loading publishable key: $e")
            } catch (e: IOException) {
                logger.error("(App) Error loading publishable key: $e")
            }
        }
    }
}

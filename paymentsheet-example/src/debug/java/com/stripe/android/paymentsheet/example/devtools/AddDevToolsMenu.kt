package com.stripe.android.paymentsheet.example.devtools

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.MenuProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.example.R
import kotlin.math.sqrt

class DevToolsActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.addDevToolsMenu()
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

fun AppCompatActivity.addDevToolsMenu(showInMenu: Boolean = false) {
    val devTools = DevToolsBottomSheetDialogFragment.newInstance()

    if (showInMenu) {
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_dev_tools -> {
                        openDevTools(devTools)
                        true
                    }
                    else -> true
                }
            }
        }

        addMenuProvider(menuProvider)
    }

    val sensorListener = object : SensorEventListener {
        private var acceleration = 0f
        private var currentAcceleration = 0f
        private var lastAcceleration = 0f

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            currentAcceleration = sqrt(x * x + y * y + z * z)
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 12) {
                openDevTools(devTools)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    val sensorManager = getSystemService<SensorManager>()!!
    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }

            override fun onPause(owner: LifecycleOwner) {
                sensorManager.unregisterListener(sensorListener)
                super.onPause(owner)
            }
        }
    )
}

private fun AppCompatActivity.openDevTools(devTools: DevToolsBottomSheetDialogFragment) {
    if (devTools.isAdded) {
        return
    }

    if (DevToolsStore.failed) {
        Toast.makeText(
            this,
            "Failed to load DevTools. Check the logs.",
            Toast.LENGTH_LONG
        ).show()
    } else {
        devTools.show(supportFragmentManager, devTools.tag)
    }
}

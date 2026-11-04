package com.stripe.android.paymentelement.confirmation.challenge

import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ChallengeHost @Inject constructor() {
    private var registration: Registration? = null

    val host: FragmentActivity?
        get() = registration?.activity

    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        unregister()
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val activity = lifecycleOwner.findActivity() ?: activityResultCaller.findActivity() ?: return
        if (
            activity.isDestroyed || activity.isFinishing ||
            activity.lifecycle.currentState == Lifecycle.State.DESTROYED
        ) {
            return
        }

        val current = Registration(activity, lifecycleOwner)
        registration = current
        activity.lifecycle.addObserver(current)
        if (lifecycleOwner !== activity) {
            lifecycleOwner.lifecycle.addObserver(current)
        }
    }

    fun launchWarmUp(block: suspend (FragmentActivity) -> Unit) {
        val current = registration ?: return
        current.scope.launch {
            if (registration === current && current.warmUpJob?.isActive != true) {
                current.warmUpJob = launch { block(current.activity) }
            }
        }
    }

    fun unregister() {
        val previous = registration ?: return
        registration = null
        previous.dispose()
    }

    private fun Any.findActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is Fragment -> activity
        else -> null
    }

    private inner class Registration(
        val activity: FragmentActivity,
        val lifecycleOwner: LifecycleOwner,
    ) : DefaultLifecycleObserver {
        private val lifecycleContext = lifecycleOwner.lifecycleScope.coroutineContext
        val scope = CoroutineScope(lifecycleContext + SupervisorJob(lifecycleContext[Job]))
        var warmUpJob: Job? = null

        fun dispose() {
            scope.cancel()
            activity.lifecycle.removeObserver(this)
            if (lifecycleOwner !== activity) {
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (registration === this) {
                unregister()
            }
        }
    }
}

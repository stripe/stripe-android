package com.stripe.android.challenge.passive.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class DefaultPassiveChallengeWarmer(
    private val hCaptchaService: HCaptchaService,
) : PassiveChallengeWarmer {
    private var owner: RegisteredOwner? = null

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        unregister()

        val activity = lifecycleOwner.findActivity() ?: activityResultCaller.findActivity()
            ?: return

        if (activity.isDestroyed || activity.isFinishing) {
            return
        }

        val newOwner = RegisteredOwner(activity)

        owner = newOwner
    }

    override fun start(passiveCaptchaParams: PassiveCaptchaParams) {
        val currentOwner = owner ?: return

        currentOwner.launchIfInactive { activity ->
            hCaptchaService.warmUp(
                activity = activity,
                siteKey = passiveCaptchaParams.siteKey,
                rqData = passiveCaptchaParams.rqData,
            )
        }
    }

    override fun unregister() {
        owner?.dispose()
        owner = null
    }

    private fun Any.findActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is Fragment -> activity
        else -> null
    }

    private class RegisteredOwner(
        private val activity: FragmentActivity,
    ) {
        private val lifecycleContext = activity.lifecycleScope.coroutineContext
        private val scope = CoroutineScope(lifecycleContext + SupervisorJob(lifecycleContext[Job]))
        private var currentJob: Job? = null

        fun launchIfInactive(block: suspend (activity: FragmentActivity) -> Unit) {
            val job = currentJob

            if (job == null || !job.isActive) {
                currentJob = scope.launch {
                    block(activity)
                }
            }
        }

        fun dispose() {
            currentJob?.cancel()
            currentJob = null
            scope.cancel()
        }
    }
}

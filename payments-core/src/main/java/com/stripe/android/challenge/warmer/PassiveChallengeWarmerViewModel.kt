package com.stripe.android.challenge.warmer

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import javax.inject.Inject

internal class PassiveChallengeWarmerViewModel @Inject constructor(
    private val passiveCaptchaParams: PassiveCaptchaParams,
    private val hCaptchaService: HCaptchaService
) : ViewModel() {

    suspend fun warmUp(activity: FragmentActivity) {
        hCaptchaService.warmUp(
            activity = activity,
            siteKey = passiveCaptchaParams.siteKey,
            rqData = passiveCaptchaParams.rqData
        )
    }

    class NoArgsException : IllegalArgumentException("No args found")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: PassiveChallengeWarmerArgs = getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                DaggerPassiveChallengeWarmerComponent
                    .builder()
                    .passiveCaptchaParams(args.passiveCaptchaParams)
                    .build()
                    .passiveChallengeWarmerViewModel
            }
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): PassiveChallengeWarmerArgs? {
            return savedStateHandle.get<PassiveChallengeWarmerArgs>(PassiveChallengeWarmerActivity.EXTRA_ARGS)
        }
    }
}

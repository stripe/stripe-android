package com.stripe.android.challenge.warmer.activity

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerActivity.Companion.getArgs
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

internal class PassiveChallengeWarmerViewModel @Inject constructor(
    private val passiveCaptchaParams: PassiveCaptchaParams,
    private val hCaptchaService: HCaptchaService,
) : ViewModel() {
    private val _result = MutableSharedFlow<PassiveChallengeWarmerCompleted>(replay = 1)
    val result: Flow<PassiveChallengeWarmerCompleted> = _result

    suspend fun warmUpPassiveChallenge(activity: FragmentActivity) {
        hCaptchaService.warmUp(
            activity = activity,
            siteKey = passiveCaptchaParams.siteKey,
            rqData = passiveCaptchaParams.rqData
        )
        _result.emit(PassiveChallengeWarmerCompleted)
    }

    class NoArgsException : IllegalArgumentException("No args found")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: PassiveChallengeWarmerArgs = getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                val app = this[APPLICATION_KEY] as Application
                DaggerPassiveChallengeWarmerActivityComponent.builder()
                    .passiveCaptchaParams(args.passiveCaptchaParams)
                    .context(app)
                    .publishableKeyProvider { args.publishableKey }
                    .productUsage(args.productUsage.toSet())
                    .build()
                    .passiveChallengeWarmerViewModel
            }
        }
    }
}

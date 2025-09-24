package com.stripe.android.challenge

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.challenge.PassiveChallengeActivity.Companion.getArgs
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class PassiveChallengeViewModel @Inject constructor(
    private val passiveCaptchaParams: PassiveCaptchaParams,
    private val hCaptchaService: HCaptchaService
) : ViewModel() {
    private val _result = MutableSharedFlow<PassiveChallengeActivityResult>(replay = 1)
    val result: Flow<PassiveChallengeActivityResult> = _result

    suspend fun startPassiveChallenge(activity: FragmentActivity) {
        val result = hCaptchaService.performPassiveHCaptcha(
            activity = activity,
            siteKey = passiveCaptchaParams.siteKey,
            rqData = passiveCaptchaParams.rqData,
            timeout = 6.seconds
        )
        when (result) {
            is HCaptchaService.Result.Failure -> {
                _result.emit(
                    value = PassiveChallengeActivityResult.Failed(result.error)
                )
            }
            is HCaptchaService.Result.Success -> {
                _result.emit(
                    value = PassiveChallengeActivityResult.Success(result.token)
                )
            }
        }
    }

    class NoArgsException : IllegalArgumentException("No args found")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: PassiveChallengeArgs = getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                val app = this[APPLICATION_KEY] as Application
                DaggerPassiveChallengeComponent
                    .builder()
                    .passiveCaptchaParams(args.passiveCaptchaParams)
                    .context(app)
                    .publishableKeyProvider { args.publishableKey }
                    .productUsage(args.productUsage.toSet())
                    .build()
                    .passiveChallengeViewModel
            }
        }
    }
}

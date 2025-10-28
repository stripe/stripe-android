package com.stripe.android.payments.core.authentication.challenge

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

internal class IntentConfirmationChallengeViewModel @Inject constructor(
    private val intentConfirmationChallenge: StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge,
    private val hCaptchaService: HCaptchaService
) : ViewModel() {
    private val _result = MutableSharedFlow<IntentConfirmationChallengeActivityResult>(replay = 1)
    val result: Flow<IntentConfirmationChallengeActivityResult> = _result

    suspend fun startIntentConfirmationChallenge(activity: FragmentActivity) {
        val vendorData = intentConfirmationChallenge.vendorData

        when (vendorData) {
            is StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.HCaptchaVendorData -> {
                performHCaptcha(activity, vendorData)
            }
            is StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.HumanSecurityVendorData -> {
                // TODO: Implement Human Security captcha
                _result.emit(
                    IntentConfirmationChallengeActivityResult.Failed(
                        UnsupportedOperationException("Human Security captcha not implemented yet")
                    )
                )
            }
            is StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.ArkoseVendorData -> {
                // TODO: Implement Arkose captcha
                _result.emit(
                    IntentConfirmationChallengeActivityResult.Failed(
                        UnsupportedOperationException("Arkose captcha not implemented yet")
                    )
                )
            }
        }
    }

    private suspend fun performHCaptcha(
        activity: FragmentActivity,
        vendorData: StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.HCaptchaVendorData
    ) {
        val result = hCaptchaService.performPassiveHCaptcha(
            activity = activity,
            siteKey = vendorData.siteKey,
            rqData = vendorData.rqData
        )
        when (result) {
            is HCaptchaService.Result.Failure -> {
                _result.emit(
                    IntentConfirmationChallengeActivityResult.Failed(result.error)
                )
            }
            is HCaptchaService.Result.Success -> {
                _result.emit(
                    IntentConfirmationChallengeActivityResult.Success(result.token)
                )
            }
        }
    }

    class NoArgsException : IllegalArgumentException("No args found")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: IntentConfirmationChallengeArgs = IntentConfirmationChallengeActivity.getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                val app = this[APPLICATION_KEY] as Application
                DaggerIntentConfirmationChallengeComponent
                    .builder()
                    .intentConfirmationChallenge(args.intentConfirmationChallenge)
                    .context(app)
                    .publishableKeyProvider { args.publishableKey }
                    .productUsage(args.productUsage.toSet())
                    .build()
                    .intentConfirmationChallengeViewModel
            }
        }
    }
}

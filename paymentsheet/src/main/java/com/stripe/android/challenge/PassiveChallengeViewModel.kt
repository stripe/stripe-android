package com.stripe.android.challenge

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.challenge.PassiveChallengeActivity.Companion.getArgs
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

internal class PassiveChallengeViewModel @Inject constructor(
    private val paymentMethodConfirmationOption: PaymentMethodConfirmationOption.New,
    private val hCaptchaService: HCaptchaService
): ViewModel() {
    private val _result = MutableSharedFlow<PassiveChallengeActivityResult>(replay = 1)
    val result: Flow<PassiveChallengeActivityResult> = _result

    suspend fun startPassiveChallenge(activity: FragmentActivity) {
        when (val result = hCaptchaService.performPassiveHCaptcha(activity)) {
            is HCaptchaService.Result.Failure -> {
                _result.emit(
                    value = PassiveChallengeActivityResult.Success(
                        newPaymentMethodConfirmationOption = paymentMethodConfirmationOption.copy(
                            createParams = paymentMethodConfirmationOption.createParams.copy(
                                radarOptions = RadarOptions("result.token")
                            )
                        )
                    )
                )
            }
            is HCaptchaService.Result.Success -> {
                _result.emit(
                    value = PassiveChallengeActivityResult.Success(
                        newPaymentMethodConfirmationOption = paymentMethodConfirmationOption.copy(
                            createParams = paymentMethodConfirmationOption.createParams.copy(
                                radarOptions = RadarOptions(result.token)
                            )
                        )
                    )
                )
            }
        }
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val args: PassiveChallengeArgs = getArgs(handle) ?: throw NoArgsException()
                DaggerChallengeComponent
                    .builder()
                    .paymentConfirmationOption(args.newPaymentMethodConfirmationOption)
                    .build()
                    .passiveChallengeViewModel
            }
        }
    }

    class NoArgsException : IllegalArgumentException("No args found")
}

package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

internal class PaymentConfirmationMediator<
    TConfirmationOption : PaymentConfirmationOption,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
>(
    private val savedStateHandle: SavedStateHandle,
    private val definition:
    PaymentConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
) {
    private var launcher: TLauncher? = null

    private val parametersKey = definition.key + PARAMETERS_POSTFIX_KEY
    private var persistedParameters: Parameters<TConfirmationOption>?
        get() = savedStateHandle[parametersKey]
        set(value) {
            savedStateHandle[parametersKey] = value
        }

    fun canConfirm(confirmationOption: PaymentConfirmationOption): Boolean {
        return definition.option(confirmationOption) != null
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        onResult: (PaymentConfirmationResult) -> Unit,
    ) {
        launcher = definition.createLauncher(
            activityResultCaller
        ) { result ->
            val confirmationResult = persistedParameters?.let { params ->
                definition.toPaymentConfirmationResult(
                    confirmationOption = params.confirmationOption,
                    intent = params.intent,
                    result = result,
                    deferredIntentConfirmationType = params.deferredIntentConfirmationType
                )
            } ?: run {
                val exception = IllegalStateException(
                    "Arguments should have been initialized before handling result!"
                )

                PaymentConfirmationResult.Failed(
                    cause = exception,
                    message = exception.stripeErrorMessage(),
                    type = PaymentConfirmationErrorType.Internal,
                )
            }

            onResult(confirmationResult)
        }
    }

    fun unregister() {
        launcher = null
    }

    suspend fun action(
        option: PaymentConfirmationOption,
        intent: StripeIntent
    ): Action {
        val confirmationOption = definition.option(option)
            ?: return Action.Fail(
                cause = IllegalArgumentException(
                    "Parameter type of '${option::class.simpleName}' cannot be used with " +
                        "${this::class.simpleName} to read a result"
                ),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = PaymentConfirmationErrorType.Internal,
            )

        return when (val action = definition.action(confirmationOption, intent)) {
            is PaymentConfirmationDefinition.ConfirmationAction.Launch -> {
                launcher?.let {
                    Action.Launch(
                        launch = {
                            persistedParameters = Parameters(
                                confirmationOption = confirmationOption,
                                intent = intent,
                                deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                            )

                            definition.launch(
                                launcher = it,
                                arguments = action.launcherArguments,
                                confirmationOption = confirmationOption,
                                intent = intent,
                            )
                        },
                    )
                } ?: run {
                    val exception = IllegalStateException(
                        "No launcher for ${definition::class.simpleName} was found, did you call register?"
                    )

                    Action.Fail(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        errorType = PaymentConfirmationErrorType.Fatal,
                    )
                }
            }
            is PaymentConfirmationDefinition.ConfirmationAction.Complete -> {
                Action.Complete(
                    intent = action.intent,
                    confirmationOption = action.confirmationOption,
                    deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                )
            }
            is PaymentConfirmationDefinition.ConfirmationAction.Fail -> {
                Action.Fail(
                    cause = action.cause,
                    message = action.message,
                    errorType = action.errorType,
                )
            }
        }
    }

    sealed interface Action {
        class Launch(
            val launch: () -> Unit,
        ) : Action

        data class Fail(
            val cause: Throwable,
            val message: ResolvableString,
            val errorType: PaymentConfirmationErrorType,
        ) : Action

        data class Complete(
            val intent: StripeIntent,
            val confirmationOption: PaymentConfirmationOption,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType? = null,
        ) : Action
    }

    @Parcelize
    internal data class Parameters<TConfirmationOption : PaymentConfirmationOption>(
        val confirmationOption: TConfirmationOption,
        val intent: StripeIntent,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    ) : Parcelable

    private companion object {
        private const val PARAMETERS_POSTFIX_KEY = "Parameters"
    }
}

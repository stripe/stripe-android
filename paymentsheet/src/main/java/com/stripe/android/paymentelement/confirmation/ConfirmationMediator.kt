package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.R
import kotlinx.parcelize.Parcelize

internal class ConfirmationMediator<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    >(
    private val savedStateHandle: SavedStateHandle,
    private val definition:
    ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
) {
    private var launcher: TLauncher? = null

    private val parametersKey = definition.key + PARAMETERS_POSTFIX_KEY
    private var persistedParameters: Parameters<TConfirmationOption>?
        get() = savedStateHandle[parametersKey]
        set(value) {
            savedStateHandle[parametersKey] = value
        }

    val key = definition.key

    fun canConfirm(
        confirmationOption: ConfirmationHandler.Option,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): Boolean {
        return definition.option(confirmationOption)?.let {
            definition.canConfirm(it, confirmationParameters)
        } ?: false
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        onResult: (ConfirmationDefinition.Result) -> Unit,
    ) {
        launcher = definition.createLauncher(
            activityResultCaller
        ) { result ->
            val confirmationResult = persistedParameters?.let { params ->
                definition.toResult(
                    confirmationOption = params.confirmationOption,
                    confirmationParameters = params.confirmationParameters,
                    result = result,
                    deferredIntentConfirmationType = params.deferredIntentConfirmationType
                )
            } ?: run {
                val exception = IllegalStateException(
                    "Arguments should have been initialized before handling result!"
                )

                ConfirmationDefinition.Result.Failed(
                    cause = exception,
                    message = exception.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                )
            }

            onResult(confirmationResult)
        }
    }

    fun unregister() {
        launcher?.let {
            definition.unregister(it)
        }

        launcher = null
    }

    suspend fun action(
        option: ConfirmationHandler.Option,
        parameters: ConfirmationDefinition.Parameters,
    ): Action {
        val confirmationOption = definition.option(option)
            ?: return Action.Fail(
                cause = IllegalArgumentException(
                    "Parameter type of '${option::class.simpleName}' cannot be used with " +
                        "${this::class.simpleName} to read a result"
                ),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )

        return when (val action = definition.action(confirmationOption, parameters)) {
            is ConfirmationDefinition.Action.Launch -> {
                launcher?.let {
                    Action.Launch(
                        launch = {
                            persistedParameters = Parameters(
                                confirmationOption = confirmationOption,
                                confirmationParameters = parameters,
                                deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                            )

                            definition.launch(
                                launcher = it,
                                arguments = action.launcherArguments,
                                confirmationOption = confirmationOption,
                                confirmationParameters = parameters,
                            )
                        },
                        receivesResultInProcess = action.receivesResultInProcess,
                    )
                } ?: run {
                    val exception = IllegalStateException(
                        "No launcher for ${definition::class.simpleName} was found, did you call register?"
                    )

                    Action.Fail(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Fatal,
                    )
                }
            }
            is ConfirmationDefinition.Action.Complete -> {
                Action.Complete(
                    intent = action.intent,
                    confirmationOption = action.confirmationOption,
                    deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    completedFullPaymentFlow = action.completedFullPaymentFlow,
                )
            }
            is ConfirmationDefinition.Action.Fail -> {
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
            val receivesResultInProcess: Boolean,
        ) : Action

        data class Fail(
            val cause: Throwable,
            val message: ResolvableString,
            val errorType: ConfirmationHandler.Result.Failed.ErrorType,
        ) : Action

        data class Complete(
            val intent: StripeIntent,
            val confirmationOption: ConfirmationHandler.Option,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType? = null,
            val completedFullPaymentFlow: Boolean,
        ) : Action
    }

    @Parcelize
    internal data class Parameters<TConfirmationOption : ConfirmationHandler.Option>(
        val confirmationOption: TConfirmationOption,
        val confirmationParameters: ConfirmationDefinition.Parameters,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    ) : Parcelable

    private companion object {
        private const val PARAMETERS_POSTFIX_KEY = "Parameters"
    }
}

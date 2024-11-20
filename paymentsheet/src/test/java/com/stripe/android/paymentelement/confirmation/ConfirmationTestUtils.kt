package com.stripe.android.paymentelement.confirmation

internal fun ConfirmationHandler.Result?.asSucceeded(): ConfirmationHandler.Result.Succeeded {
    return this as ConfirmationHandler.Result.Succeeded
}

internal fun ConfirmationHandler.Result?.asFailed(): ConfirmationHandler.Result.Failed {
    return this as ConfirmationHandler.Result.Failed
}

internal fun ConfirmationHandler.Result?.asCanceled(): ConfirmationHandler.Result.Canceled {
    return this as ConfirmationHandler.Result.Canceled
}

internal fun <T> ConfirmationDefinition.ConfirmationAction<T>.asFail():
    ConfirmationDefinition.ConfirmationAction.Fail<T> {
    return this as ConfirmationDefinition.ConfirmationAction.Fail<T>
}

internal fun <T> ConfirmationDefinition.ConfirmationAction<T>.asLaunch():
    ConfirmationDefinition.ConfirmationAction.Launch<T> {
    return this as ConfirmationDefinition.ConfirmationAction.Launch<T>
}

internal fun ConfirmationMediator.Action.asLaunch(): ConfirmationMediator.Action.Launch {
    return this as ConfirmationMediator.Action.Launch
}

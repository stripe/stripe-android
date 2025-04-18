package com.stripe.android.paymentelement.confirmation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MainThreadConfirmationHandler(
    private val delegate: ConfirmationHandler,
) : ConfirmationHandler by delegate {
    override suspend fun start(arguments: ConfirmationHandler.Args) {
        withContext(Dispatchers.Main) {
            delegate.start(arguments)
        }
    }
}

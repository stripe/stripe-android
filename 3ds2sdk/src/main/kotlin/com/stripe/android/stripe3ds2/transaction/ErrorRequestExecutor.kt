package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transactions.ErrorData

internal fun interface ErrorRequestExecutor {
    fun executeAsync(errorData: ErrorData)

    fun interface Factory {
        fun create(
            acsUrl: String,
            errorReporter: ErrorReporter
        ): ErrorRequestExecutor
    }
}

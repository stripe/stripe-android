package com.stripe.android.financialconnections.exception

internal class WebAuthFlowCancelledException : Exception()
internal class WebAuthFlowFailedException(val url: String?): Exception()

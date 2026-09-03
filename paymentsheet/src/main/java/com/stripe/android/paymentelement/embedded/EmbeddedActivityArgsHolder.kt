package com.stripe.android.paymentelement.embedded

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class EmbeddedActivityArgsHolder(
    initialArgs: EmbeddedActivityArgs,
) {
    private val _args = MutableStateFlow(initialArgs)
    val args: StateFlow<EmbeddedActivityArgs> = _args.asStateFlow()

    fun update(args: EmbeddedActivityArgs) {
        _args.value = args
    }
}

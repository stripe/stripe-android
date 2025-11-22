package com.stripe.android.common.taptoadd

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener
import javax.inject.Inject

internal interface TerminalWrapper {
    fun isInitialized(): Boolean

    fun initTerminal(
        context: Context,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener,
    )

    fun getInstance(): Terminal
}

internal class DefaultTerminalWrapper @Inject constructor() : TerminalWrapper {
    override fun isInitialized(): Boolean {
        return Terminal.isInitialized()
    }

    override fun initTerminal(
        context: Context,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener,
    ) {
        Terminal.initTerminal(
            context = context,
            tokenProvider = tokenProvider,
            listener = listener,
        )
    }

    override fun getInstance(): Terminal {
        return Terminal.getInstance()
    }
}

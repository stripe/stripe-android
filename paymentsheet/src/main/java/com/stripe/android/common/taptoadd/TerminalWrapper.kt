package com.stripe.android.common.taptoadd

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener

internal interface TerminalWrapper {
    fun isInitialized(): Boolean

    fun initTerminal(
        context: Context,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener,
    )

    fun getInstance(): Terminal

    companion object {
        @VisibleForTesting
        @Volatile
        var overrideWrapper: TerminalWrapper? = null

        fun create(): TerminalWrapper {
            return overrideWrapper ?: DefaultTerminalWrapper()
        }
    }
}

internal class DefaultTerminalWrapper : TerminalWrapper {
    override fun isInitialized(): Boolean {
        return Terminal.isInitialized()
    }

    override fun initTerminal(
        context: Context,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener,
    ) {
        Terminal.init(
            context = context,
            tokenProvider = tokenProvider,
            offlineListener = null,
            listener = listener,
        )
    }

    override fun getInstance(): Terminal {
        return Terminal.getInstance()
    }
}

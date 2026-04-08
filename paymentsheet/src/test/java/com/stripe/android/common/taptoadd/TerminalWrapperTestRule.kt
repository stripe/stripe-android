package com.stripe.android.common.taptoadd

import android.content.Context
import com.stripe.android.tta.testing.TerminalTestDelegate
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TerminalWrapperTestRule : TestWatcher() {
    private lateinit var _delegate: TerminalTestDelegate
    val delegate: TerminalTestDelegate
        get() = _delegate

    override fun starting(description: Description?) {
        _delegate = TerminalTestDelegate()
        TerminalWrapper.overrideWrapper = FakeTerminalWrapper(delegate)
        super.starting(description)
    }

    override fun finished(description: Description?) {
        TerminalWrapper.overrideWrapper = null

        if (delegate.shouldValidate) {
            delegate.validate()
        }

        super.finished(description)
    }

    private class FakeTerminalWrapper(
        private val delegate: TerminalTestDelegate
    ) : TerminalWrapper {
        override fun isInitialized(): Boolean {
            return delegate.isInitialized()
        }

        override fun initTerminal(
            context: Context,
            tokenProvider: ConnectionTokenProvider,
            listener: TerminalListener
        ) {
            delegate.initTerminal(context, tokenProvider, listener)
        }

        override fun getInstance(): Terminal {
            return delegate.getInstance()
        }
    }
}

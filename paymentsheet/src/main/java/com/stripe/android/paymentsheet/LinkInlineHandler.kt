package com.stripe.android.paymentsheet

import com.stripe.android.link.ui.inline.InlineSignupViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LinkInlineHandler private constructor() {
    private val _linkInlineState = MutableStateFlow<InlineSignupViewState?>(null)
    val linkInlineState: StateFlow<InlineSignupViewState?> = _linkInlineState

    fun onStateUpdated(viewState: InlineSignupViewState?) {
        _linkInlineState.value = viewState
    }

    companion object {
        fun create() = LinkInlineHandler()
    }
}

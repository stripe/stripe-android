package com.stripe.android.paymentelement.embedded.form

import javax.inject.Singleton

internal interface OnClickOverrideDelegate {
    val onClickOverride: (() -> Unit)?
    fun set(onClick: () -> Unit)
    fun clear()
}

@Singleton
internal class OnClickDelegateOverrideImpl : OnClickOverrideDelegate {
    private var _onClickOverride: (() -> Unit)? = null
    override val onClickOverride: (() -> Unit)?
        get() = _onClickOverride

    override fun set(onClick: () -> Unit) {
        _onClickOverride = onClick
    }

    override fun clear() {
        _onClickOverride = null
    }
}

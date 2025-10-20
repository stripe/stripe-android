package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat

internal class ViewActionRecorder<VA> {
    private val _viewActions: MutableList<VA> = mutableListOf()
    val viewActions: List<VA>
        get() = _viewActions.toList()

    fun record(viewAction: VA) {
        _viewActions += viewAction
    }

    fun consume(viewAction: VA) {
        assertThat(_viewActions.size).isGreaterThan(0)
        assertThat(_viewActions[0]).isEqualTo(viewAction)
        _viewActions.removeAt(0)
    }

    fun consume(
        criteria: (VA) -> Boolean,
    ) {
        assertThat(_viewActions.size).isGreaterThan(0)
        criteria(_viewActions[0])
        _viewActions.removeAt(0)
    }
}

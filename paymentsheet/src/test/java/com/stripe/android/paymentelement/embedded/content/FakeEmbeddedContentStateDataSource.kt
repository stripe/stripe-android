package com.stripe.android.paymentelement.embedded.content

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeEmbeddedContentStateDataSource(
    private val mutableEmbeddedContentState: MutableStateFlow<EmbeddedContentState?> = MutableStateFlow(null),
) : EmbeddedContentStateDataSource {
    override val embeddedContentState: StateFlow<EmbeddedContentState?> = mutableEmbeddedContentState

    fun updateState(state: EmbeddedContentState?) {
        mutableEmbeddedContentState.value = state
    }
}

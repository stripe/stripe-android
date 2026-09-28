package com.stripe.android.financialconnections.domain

import com.stripe.android.model.LinkBrand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeCurrentLinkBrand(
    initialLinkBrand: LinkBrand = LinkBrand.Link,
) : CurrentLinkBrand {
    private val _stateFlow = MutableStateFlow(initialLinkBrand)

    override val stateFlow: StateFlow<LinkBrand> = _stateFlow.asStateFlow()

    override fun invoke(): LinkBrand {
        return stateFlow.value
    }

    fun set(linkBrand: LinkBrand) {
        _stateFlow.value = linkBrand
    }
}

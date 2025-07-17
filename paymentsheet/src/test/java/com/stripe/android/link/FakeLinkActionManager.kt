package com.stripe.android.link

import kotlinx.coroutines.flow.MutableSharedFlow

internal fun createTestLinkActions(): LinkActions = MutableSharedFlow(extraBufferCapacity = 1)

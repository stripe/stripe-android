package com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader

import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ui.common.Async
import com.stripe.android.connect.example.ui.common.Uninitialized

@OptIn(PrivateBetaConnectSDK::class)
data class EmbeddedComponentManagerLoaderState(
    val embeddedComponentManagerAsync: Async<EmbeddedComponentManager> = Uninitialized,
)
package com.stripe.android.connect

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

@OptIn(PrivateBetaConnectSDK::class)
class EmptyActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmbeddedComponentManager.onActivityCreate(this)
    }
}

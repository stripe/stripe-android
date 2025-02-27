package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
abstract class BaseActivity : FragmentActivity() {

    protected val loaderViewModel: EmbeddedComponentLoaderViewModel by viewModels()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbeddedComponentManager.onActivityCreate(this)
        lifecycle.addObserver(loaderViewModel)
    }
}

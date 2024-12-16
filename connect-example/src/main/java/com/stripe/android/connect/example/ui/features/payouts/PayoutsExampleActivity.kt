package com.stripe.android.connect.example.ui.features.payouts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmptyProps
import com.stripe.android.connect.PayoutsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.databinding.ViewPayoutsExampleBinding
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PayoutsExampleActivity : BasicExampleComponentActivity() {
    override val titleRes: Int = R.string.payouts

    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val settings = settingsViewModel.state.value
        val listener = Listener()
        return if (settings.presentationSettings.useXmlViews) {
            ViewPayoutsExampleBinding.inflate(LayoutInflater.from(context)).root
                .apply {
                    initialize(
                        embeddedComponentManager = embeddedComponentManager,
                        listener = listener,
                        props = EmptyProps
                    )
                }
        } else {
            embeddedComponentManager.createPayoutsView(
                context = context,
                listener = listener,
            )
        }
    }

    private inner class Listener : PayoutsListener {
        override fun onLoadError(error: Throwable) {
            Toast.makeText(this@PayoutsExampleActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }
}

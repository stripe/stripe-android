package com.stripe.android.connect.example.ui.features.payments

import android.content.Context
import android.view.View
import android.widget.Toast
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PaymentsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PaymentsExampleActivity : BasicExampleComponentActivity() {

    @Inject
    lateinit var settingsService: SettingsService

    override val titleRes: Int = R.string.payments

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val listener = Listener()
        val paymentsSettings = settingsService.getPaymentsSettings()
        return embeddedComponentManager.createPaymentsView(
            context = context,
            listener = listener,
            props = paymentsSettings.toProps(),
            cacheKey = "PaymentsExampleActivity"
        )
    }

    private inner class Listener : PaymentsListener {
        override fun onLoadError(error: Throwable) {
            Toast.makeText(this@PaymentsExampleActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }
}

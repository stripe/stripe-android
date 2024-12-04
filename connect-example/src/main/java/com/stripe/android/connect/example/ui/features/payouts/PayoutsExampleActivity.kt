package com.stripe.android.connect.example.ui.features.payouts

import android.content.Context
import android.view.View
import android.widget.Toast
import com.stripe.android.connect.PayoutsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BasicComponentExampleActivity
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PayoutsExampleActivity : BasicComponentExampleActivity() {
    override val titleRes: Int = R.string.payouts

    override fun createComponentView(context: Context): View {
        return embeddedComponentManager.createPayoutsView(
            context = context,
            listener = Listener(),
        )
    }

    private inner class Listener : PayoutsListener {
        override fun onLoadError(error: Throwable) {
            Toast.makeText(this@PayoutsExampleActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }
}

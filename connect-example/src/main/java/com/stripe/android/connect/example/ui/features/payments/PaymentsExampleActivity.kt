package com.stripe.android.connect.example.ui.features.payments

import android.content.Context
import android.view.View
import android.widget.Toast
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PaymentsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PaymentsExampleActivity : BasicExampleComponentActivity() {
    override val titleRes: Int = R.string.payments

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val listener = Listener()
        return embeddedComponentManager.createPaymentsView(
            context = context,
            listener = listener,
            cacheKey = "PaymentsExampleActivity"
        )
    }

    private inner class Listener : PaymentsListener {
        override fun onLoadError(error: Throwable) {
            Toast.makeText(this@PaymentsExampleActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }
} 

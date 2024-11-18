package com.stripe.android.connect.example.ui.features.payouts

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import android.content.Context
import android.view.View
import android.widget.Toast
import com.stripe.android.connect.PayoutsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BasicComponentExampleActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PayoutsExampleActivity : BasicComponentExampleActivity() {
    override val titleRes: Int = R.string.payouts

    override fun createComponentView(context: Context): View {
        return embeddedComponentManager.createPayoutsView(
            activity = this@PayoutsExampleActivity,
            listener = Listener(),
        )
    }

    private inner class Listener : PayoutsListener {
        override fun onLoadError(error: Throwable) {
            Toast.makeText(this@PayoutsExampleActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }
}

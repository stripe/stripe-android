package com.stripe.android.connect.example.ui.features.payouts

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PayoutsView
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ConnectSdkExampleTheme
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BackIconButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class PayoutsExampleActivity : FragmentActivity() {

    @Inject private lateinit var embeddedComponentManager: EmbeddedComponentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                MainContent(
                    title = stringResource(R.string.payouts),
                    navigationIcon = {
                        BackIconButton(onClick = this@PayoutsExampleActivity::finish)
                    }
                ) {
                    PayoutsComponentWrapper(onDismiss = this@PayoutsExampleActivity::finish)
                }
            }
        }
    }

    @OptIn(PrivateBetaConnectSDK::class)
    @Composable
    private fun PayoutsComponentWrapper(onDismiss: () -> Unit) {
        BackHandler(onBack = onDismiss)
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            embeddedComponentManager.createPayoutsView(context)
        })
    }
}

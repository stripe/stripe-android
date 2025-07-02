package com.stripe.android.paymentsheet.example.samples.ui.link

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkPaymentMethodLauncher
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

internal class LinkStandaloneActivity : AppCompatActivity() {

    private lateinit var linkPaymentMethodLauncher: LinkPaymentMethodLauncher

    @SuppressWarnings("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FeatureFlags.nativeLinkEnabled.setEnabled(true)

        linkPaymentMethodLauncher = LinkPaymentMethodLauncher.create(this)

        setContent {
            PaymentSheetExampleTheme {
                var email by remember { mutableStateOf("") }
                Column {
                    TextField(
                        value = email,
                        onValueChange = { email = it }
                    )
                    Button(onClick = { linkPaymentMethodLauncher.present(email) }) {
                        Text("Launch")
                    }
                }
            }
        }
    }
}

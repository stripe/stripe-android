package com.stripe.android.paymentsheet.example.samples.ui.link

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                var currentPreview by remember { mutableStateOf<LinkPaymentMethodLauncher.PaymentMethodPreview?>(null) }

                LaunchedEffect(Unit) {
                    linkPaymentMethodLauncher.listener =
                        object : LinkPaymentMethodLauncher.Listener {
                            override fun onPaymentMethodSelected(
                                preview: LinkPaymentMethodLauncher.PaymentMethodPreview
                            ) {
                                currentPreview = preview
                            }
                        }
                }

                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField(
                        value = email,
                        placeholder = { Text(text = "Email") },
                        onValueChange = { email = it }
                    )
                    Button(onClick = { linkPaymentMethodLauncher.present(email) }) {
                        Text("Launch")
                    }
                    currentPreview?.let { preview ->
                        Text(text = "Payment method:")
                        Text(text = preview.label)
                        Text(text = preview.sublabel ?: "")
                    }
                }
            }
        }
    }
}

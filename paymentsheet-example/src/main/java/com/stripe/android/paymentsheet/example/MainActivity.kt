package com.stripe.android.paymentsheet.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentsheet.example.databinding.ActivityMainBinding
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.samples.ui.complete_flow.CompleteFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.custom_flow.CustomFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.server_side_confirm.ServerSideConfirmationActivity

private const val SurfaceOverlayOpacity = 0.12f

class MainActivity : AppCompatActivity() {

    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val items: List<MenuItem> by lazy {
        listOf(
            MenuItem(
                title = "Basic PaymentSheet",
                subtitle = "Our simplest integration",
                klass = CompleteFlowActivity::class.java,
            ),
            MenuItem(
                title = "PaymentSheet with FlowController",
                subtitle = "A more advanced integration with greater flexibility",
                klass = CustomFlowActivity::class.java,
            ),
            MenuItem(
                title = "PaymentSheet with server-side confirmation",
                subtitle = "Create and confirm the payment or setup intent on your own backend",
                klass = ServerSideConfirmationActivity::class.java,
                isBeta = true,
            ),
            MenuItem(
                title = "Playground",
                subtitle = "Testing playground for Stripe engineers",
                klass = PaymentSheetPlaygroundActivity::class.java,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)

        viewBinding.content.setContent {
            MainScreen(items = items)
        }
    }
}

private data class MenuItem(
    val title: String,
    val subtitle: String,
    val klass: Class<out ComponentActivity>,
    val isBeta: Boolean = false,
)

@Composable
private fun MainScreen(items: List<MenuItem>) {
    Column {
        LazyColumn {
            items(items) { item ->
                MenuItemRow(item)
                Divider(startIndent = 16.dp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(text = StripeSdkVersion.VERSION_NAME)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MenuItemRow(item: MenuItem) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.startActivity(Intent(context, item.klass)) }
            .padding(16.dp),
    ) {
        Text(
            text = item.title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Text(text = item.subtitle)

        if (item.isBeta) {
            Chip(
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.secondary.copy(
                        alpha = SurfaceOverlayOpacity,
                    ),
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                    )
                },
                onClick = context::openDecouplingBetaLink,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(text = "This is currently in beta.")
            }
        }
    }
}

private fun Context.openDecouplingBetaLink() {
    val url = "https://stripe.com/docs/payments/finalize-payments-on-the-server?platform=mobile"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    startActivity(intent)
}

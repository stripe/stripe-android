package com.stripe.android.paymentsheet.example

import android.content.Intent
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
import androidx.compose.material.Divider
import androidx.compose.material.Text
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
    }
}

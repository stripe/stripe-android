package com.stripe.android.paymentmethodmessaging.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {

    private val items: List<MenuItem> by lazy {
        listOf(
            MenuItem(
                titleResId = R.string.payment_method_messaging_title,
                subtitleResId = R.string.payment_method_messaging_subtitle,
                klass = MessagingElementActivity::class.java,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            MaterialTheme {
                MainScreen(items = items)
            }
        }
    }
}

private data class MenuItem(
    val titleResId: Int,
    val subtitleResId: Int,
    val klass: Class<out ComponentActivity>,
)

@Composable
private fun MainScreen(items: List<MenuItem>) {
    LazyColumn {
        items(items) {
            MenuItemRow(it)
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
            text = stringResource(item.titleResId),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 2.dp),
            color = MaterialTheme.colors.onSurface,
        )

        Text(
            text = stringResource(item.subtitleResId),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
    }
}

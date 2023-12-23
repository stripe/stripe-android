package com.stripe.android.financialconnections.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.core.version.StripeSdkVersion

private const val SurfaceOverlayOpacity = 0.12f

class FinancialConnectionsLauncherActivity : AppCompatActivity() {

    private val items: List<MenuItem> by lazy {
        listOf(
            MenuItem(
                titleResId = R.string.financial_connections_data_kotlin_title,
                subtitleResId = R.string.financial_connections_data_subtitle,
                klass = FinancialConnectionsDataExampleActivity::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),

            MenuItem(
                titleResId = R.string.financial_connections_data_compose_title,
                subtitleResId = R.string.financial_connections_data_subtitle,
                klass = FinancialConnectionsComposeExampleActivity::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),

            MenuItem(
                titleResId = R.string.financial_connections_data_java_title,
                subtitleResId = R.string.financial_connections_data_subtitle,
                klass = FinancialConnectionsDataExampleActivityJava::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),

            MenuItem(
                titleResId = R.string.financial_connections_payouts_title,
                subtitleResId = R.string.financial_connections_payouts_subtitle,
                klass = FinancialConnectionsBankAccountTokenExampleActivity::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),

            MenuItem(
                titleResId = R.string.financial_connections_webview_title,
                subtitleResId = R.string.financial_connections_webview_subtitle,
                klass = FinancialConnectionsWebviewExampleActivity::class.java,
                section = MenuItem.Section.Alternative,
            ),

            MenuItem(
                titleResId = R.string.financial_connections_playground_title,
                subtitleResId = R.string.financial_connections_playground_subtitle,
                klass = FinancialConnectionsPlaygroundActivity::class.java,
                section = MenuItem.Section.Internal,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinancialConnectionsExampleTheme {
                MainScreen(items = items)
            }
        }
    }

    @Composable
    private fun MainScreen(items: List<MenuItem>) {
        val groupedItems = remember(items) {
            items.groupBy(MenuItem::section)
        }

        LazyColumn {
            Section(
                title = "Recommended Integrations",
                items = groupedItems.getOrElse(MenuItem.Section.CompleteFlow) { emptyList() },
            )

            Section(
                title = "Alternatives",
                badge = { NotRecommended() },
                items = groupedItems.getOrElse(MenuItem.Section.Alternative) { emptyList() },
            )

            Section(
                title = "Internal",
                items = groupedItems.getOrElse(MenuItem.Section.Internal) { emptyList() },
            )

            item {
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Version ${StripeSdkVersion.VERSION_NAME}",
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterialApi::class)
    private fun NotRecommended() {
        Chip(
            colors = ChipDefaults.chipColors(
                backgroundColor = MaterialTheme.colors.error.copy(
                    alpha = SurfaceOverlayOpacity,
                ),
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                )
            },
            onClick = { },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(text = "Not recommended")
        }
    }

    private fun LazyListScope.Section(
        title: String,
        items: List<MenuItem>,
        badge: @Composable () -> Unit = { },
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .alpha(.7f),
                )
                Spacer(modifier = Modifier.size(16.dp))
                badge()
            }
        }

        itemsIndexed(items) { index, item ->
            MenuItemRow(item)

            if (index < items.lastIndex) {
                Divider(startIndent = 16.dp)
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

    private data class MenuItem(
        val titleResId: Int,
        val subtitleResId: Int,
        val klass: Class<out ComponentActivity>,
        val section: Section,
    ) {

        enum class Section {
            CompleteFlow,
            Alternative,
            Internal,
        }
    }
}

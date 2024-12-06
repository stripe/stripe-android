package com.stripe.android.paymentsheet.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Info
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
import com.stripe.android.paymentsheet.example.databinding.ActivityMainBinding
import com.stripe.android.paymentsheet.example.playground.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.samples.ui.SECTION_ALPHA
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.AddressElementExampleActivity
import com.stripe.android.paymentsheet.example.samples.ui.customersheet.CustomerSheetExampleActivity
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.complete_flow.CompleteFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow.CustomFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.complete_flow.ServerSideConfirmationCompleteFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow.ServerSideConfirmationCustomFlowActivity
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

private const val SurfaceOverlayOpacity = 0.12f

class MainActivity : AppCompatActivity() {

    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val items: List<MenuItem> by lazy {
        listOf(
            MenuItem(
                titleResId = R.string.playground_title,
                subtitleResId = R.string.playground_subtitle,
                klass = PaymentSheetPlaygroundActivity::class.java,
                section = MenuItem.Section.Internal,
            ),
            MenuItem(
                titleResId = R.string.paymentsheet_title,
                subtitleResId = R.string.paymentsheet_subtitle,
                klass = CompleteFlowActivity::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),
            MenuItem(
                titleResId = R.string.paymentsheet_custom_title,
                subtitleResId = R.string.paymentsheet_custom_subtitle,
                klass = CustomFlowActivity::class.java,
                section = MenuItem.Section.CustomFlow,
            ),
            MenuItem(
                titleResId = R.string.paymentsheet_serverside_confirmation_title,
                subtitleResId = R.string.paymentsheet_serverside_confirmation_subtitle,
                klass = ServerSideConfirmationCompleteFlowActivity::class.java,
                section = MenuItem.Section.CompleteFlow,
            ),
            MenuItem(
                titleResId = R.string.paymentsheet_custom_serverside_confirmation_title,
                subtitleResId = R.string.paymentsheet_serverside_confirmation_subtitle,
                klass = ServerSideConfirmationCustomFlowActivity::class.java,
                section = MenuItem.Section.CustomFlow,
            ),
            MenuItem(
                titleResId = R.string.customersheet_example_title,
                subtitleResId = R.string.customer_subtitle,
                klass = CustomerSheetExampleActivity::class.java,
                section = MenuItem.Section.CustomerSheet,
            ),
            MenuItem(
                titleResId = R.string.address_element_title,
                subtitleResId = R.string.address_element_subtitle,
                klass = AddressElementExampleActivity::class.java,
                section = MenuItem.Section.AddressElement,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        viewBinding.content.setContent {
            PaymentSheetExampleTheme {
                MainScreen(items = items)
            }
        }
    }
}

private data class MenuItem(
    val titleResId: Int,
    val subtitleResId: Int,
    val klass: Class<out ComponentActivity>,
    val badge: Badge? = null,
    val section: Section,
) {
    data class Badge(
        val labelResId: Int,
        val onClick: () -> Unit = { },
    )

    enum class Section {
        Internal,
        CompleteFlow,
        CustomFlow,
        CustomerSheet,
        AddressElement,
    }
}

@Composable
private fun MainScreen(items: List<MenuItem>) {
    val groupedItems = remember(items) {
        items.groupBy(MenuItem::section)
    }

    LazyColumn {
        Section(
            title = "Internal",
            items = groupedItems.getOrElse(MenuItem.Section.Internal) { emptyList() },
        )

        Section(
            title = "Complete Flow",
            items = groupedItems.getOrElse(MenuItem.Section.CompleteFlow) { emptyList() },
        )

        Section(
            title = "Custom Flow",
            items = groupedItems.getOrElse(MenuItem.Section.CustomFlow) { emptyList() },
        )

        Section(
            title = "Customer Sheet",
            items = groupedItems.getOrElse(MenuItem.Section.CustomerSheet) { emptyList() }
        )

        Section(
            title = "Address Element",
            items = groupedItems.getOrElse(MenuItem.Section.AddressElement) { emptyList() }
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

private fun LazyListScope.Section(
    title: String,
    items: List<MenuItem>,
) {
    item {
        Text(
            text = title,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .alpha(SECTION_ALPHA)
                .padding(top = 16.dp, start = 16.dp),
        )
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

        if (item.badge != null) {
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
                onClick = item.badge.onClick,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(text = stringResource(item.badge.labelResId))
            }
        }
    }
}
